package com.cybersocial.friend.dto;

import com.cybersocial.friend.Friendship;
import com.cybersocial.friend.FriendshipStatus;
import com.cybersocial.user.User;
import java.util.UUID;

public record FriendUserResponse(
        UUID id,
        String displayName,
        String avatarUrl,
        String coverUrl,
        FriendshipStatus relationshipStatus,
        UUID friendshipId
) {
    public static FriendUserResponse from(User user, Friendship friendship) {
        return new FriendUserResponse(
                user.getId(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getCoverUrl(),
                friendship == null ? null : friendship.getStatus(),
                friendship == null ? null : friendship.getId()
        );
    }
}
