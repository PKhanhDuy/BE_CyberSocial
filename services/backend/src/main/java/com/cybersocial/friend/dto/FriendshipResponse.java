package com.cybersocial.friend.dto;

import com.cybersocial.friend.Friendship;
import com.cybersocial.friend.FriendshipStatus;
import com.cybersocial.user.User;
import java.time.Instant;
import java.util.UUID;

public record FriendshipResponse(
        UUID id,
        FriendshipStatus status,
        UUID requesterId,
        UUID addresseeId,
        FriendUserResponse user,
        Instant createdAt,
        Instant updatedAt
) {
    public static FriendshipResponse from(Friendship friendship, UUID currentUserId) {
        User otherUser = friendship.getRequester().getId().equals(currentUserId)
                ? friendship.getAddressee()
                : friendship.getRequester();
        return new FriendshipResponse(
                friendship.getId(),
                friendship.getStatus(),
                friendship.getRequester().getId(),
                friendship.getAddressee().getId(),
                FriendUserResponse.from(otherUser, friendship),
                friendship.getCreatedAt(),
                friendship.getUpdatedAt()
        );
    }
}
