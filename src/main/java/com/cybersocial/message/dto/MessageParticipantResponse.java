package com.cybersocial.message.dto;

import com.cybersocial.user.User;
import java.util.UUID;

public record MessageParticipantResponse(
        UUID id,
        String displayName,
        String avatarUrl
) {
    public static MessageParticipantResponse from(User user) {
        return new MessageParticipantResponse(
                user.getId(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }
}
