package com.cybersocial.follow.dto;

import com.cybersocial.follow.UserFollow;
import com.cybersocial.user.User;
import java.time.Instant;
import java.util.UUID;

public record FollowUserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        Instant followedAt
) {
    public static FollowUserResponse fromFollower(User user, Instant followedAt) {
        return new FollowUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                followedAt
        );
    }

    public static FollowUserResponse fromFollowRecord(UserFollow follow) {
        return fromFollower(follow.getFollower(), follow.getCreatedAt());
    }
}
