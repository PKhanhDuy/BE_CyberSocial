package com.cybersocial.friend;

import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ConflictException;
import com.cybersocial.common.exception.ForbiddenOperationException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.friend.dto.FriendUserResponse;
import com.cybersocial.friend.dto.FriendshipResponse;
import com.cybersocial.notification.NotificationService;
import com.cybersocial.notification.NotificationType;
import com.cybersocial.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FriendshipServiceImpl(
            FriendshipRepository friendshipRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> findFriends(UUID currentUserId) {
        return friendshipRepository.findByParticipantAndStatus(currentUserId, FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> FriendshipResponse.from(friendship, currentUserId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> findIncomingRequests(UUID currentUserId) {
        return friendshipRepository.findIncomingPending(currentUserId)
                .stream()
                .map(friendship -> FriendshipResponse.from(friendship, currentUserId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> findOutgoingRequests(UUID currentUserId) {
        return friendshipRepository.findOutgoingPending(currentUserId)
                .stream()
                .map(friendship -> FriendshipResponse.from(friendship, currentUserId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<FriendUserResponse> searchUsers(UUID currentUserId, String query, Pageable pageable) {
        String normalizedQuery = query == null ? "" : query.trim();
        Page<FriendUserResponse> page = userRepository.searchUsers(currentUserId, normalizedQuery, pageable)
                .map(user -> FriendUserResponse.from(user, friendshipRepository.findBetween(currentUserId, user.getId()).orElse(null)));

        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Override
    @Transactional
    public FriendshipResponse sendRequest(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("You cannot send a friend request to yourself");
        }

        User requester = getUser(currentUserId);
        User addressee = getUser(targetUserId);
        return friendshipRepository.findBetween(currentUserId, targetUserId)
                .map(existing -> handleExistingRequest(currentUserId, existing))
                .orElseGet(() -> {
                    Friendship friendship = friendshipRepository.save(Friendship.builder()
                            .requester(requester)
                            .addressee(addressee)
                            .status(FriendshipStatus.PENDING)
                            .build());
                    notificationService.create(
                            addressee,
                            NotificationType.POST,
                            "Lời mời kết bạn mới",
                            requester.getDisplayName() + " đã gửi cho bạn một lời mời kết bạn."
                    );
                    return FriendshipResponse.from(friendship, currentUserId);
                });
    }

    @Override
    @Transactional
    public FriendshipResponse acceptRequest(UUID currentUserId, UUID requestId) {
        Friendship friendship = getFriendship(requestId);
        if (!friendship.getAddressee().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You can only accept friend requests sent to you");
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BadRequestException("Friend request is not pending");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        return FriendshipResponse.from(friendship, currentUserId);
    }

    @Override
    @Transactional
    public void deleteRequest(UUID currentUserId, UUID requestId) {
        Friendship friendship = getFriendship(requestId);
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BadRequestException("Friend request is not pending");
        }
        ensureParticipant(currentUserId, friendship);
        friendshipRepository.delete(friendship);
    }

    @Override
    @Transactional
    public void removeFriend(UUID currentUserId, UUID friendshipId) {
        Friendship friendship = getFriendship(friendshipId);
        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new BadRequestException("Friendship is not accepted");
        }
        ensureParticipant(currentUserId, friendship);
        friendshipRepository.delete(friendship);
    }

    private FriendshipResponse handleExistingRequest(UUID currentUserId, Friendship existing) {
        if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
            throw new ConflictException("You are already friends");
        }
        if (existing.getRequester().getId().equals(currentUserId)) {
            throw new ConflictException("Friend request already sent");
        }

        existing.setStatus(FriendshipStatus.ACCEPTED);
        return FriendshipResponse.from(existing, currentUserId);
    }

    private void ensureParticipant(UUID currentUserId, Friendship friendship) {
        if (!friendship.getRequester().getId().equals(currentUserId)
                && !friendship.getAddressee().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You are not part of this friendship");
        }
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Friendship getFriendship(UUID friendshipId) {
        return friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));
    }
}
