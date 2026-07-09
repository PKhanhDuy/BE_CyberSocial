package com.cybersocial.follow;

import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ConflictException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.follow.dto.FollowCountResponse;
import com.cybersocial.follow.dto.FollowStatusResponse;
import com.cybersocial.follow.dto.FollowUserResponse;
import com.cybersocial.user.User;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowServiceImpl(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public FollowUserResponse follow(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("You cannot follow yourself");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {
            throw new ConflictException("Already following this user");
        }

        UserFollow follow = followRepository.save(UserFollow.builder()
                .follower(userRepository.getReferenceById(currentUserId))
                .following(targetUser)
                .build());

        return FollowUserResponse.fromFollower(targetUser, follow.getCreatedAt());
    }

    @Override
    @Transactional
    public void cancelFollow(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("You cannot unfollow yourself");
        }

        if (!userRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException("User not found");
        }

        if (!followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {
            throw new ResourceNotFoundException("Follow relationship not found");
        }

        followRepository.deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowCountResponse countFollowers(UUID userId) {
        ensureUserExists(userId);
        return new FollowCountResponse(followRepository.countByFollowingId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public FollowCountResponse countFollowing(UUID userId) {
        ensureUserExists(userId);
        return new FollowCountResponse(followRepository.countByFollowerId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<FollowUserResponse> getFollowers(UUID userId, Pageable pageable) {
        ensureUserExists(userId);
        Page<FollowUserResponse> page = followRepository.findFollowersByUserId(userId, pageable)
                .map(FollowUserResponse::fromFollowRecord);
        return toPagedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<FollowUserResponse> getFollowing(UUID userId, Pageable pageable) {
        ensureUserExists(userId);
        Page<FollowUserResponse> page = followRepository.findFollowingByUserId(userId, pageable)
                .map(follow -> FollowUserResponse.fromFollower(follow.getFollowing(), follow.getCreatedAt()));
        return toPagedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusResponse isFollowing(UUID currentUserId, UUID targetUserId) {
        ensureUserExists(targetUserId);
        return new FollowStatusResponse(
                followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)
        );
    }

    private void ensureUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
    }

    private PagedResponse<FollowUserResponse> toPagedResponse(Page<FollowUserResponse> page) {
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
}
