package com.cybersocial.story.dto;

import com.cybersocial.user.User;
import java.util.UUID;

public record StoryAuthorResponse(
        UUID id,
        String displayName,
        String avatarUrl
) {
    public static StoryAuthorResponse from(User user) {
        return new StoryAuthorResponse(
                user.getId(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }
}
