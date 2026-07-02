package com.cybersocial.message.dto;

import com.cybersocial.message.MessageReaction;
import java.time.Instant;
import java.util.UUID;

public record MessageReactionResponse(
        UUID id,
        UUID messageId,
        UUID userId,
        String displayName,
        String avatarUrl,
        String emoji,
        Instant createdAt,
        Instant updatedAt
) {
    public static MessageReactionResponse from(MessageReaction reaction) {
        return new MessageReactionResponse(
                reaction.getId(),
                reaction.getMessage().getId(),
                reaction.getUser().getId(),
                reaction.getUser().getDisplayName(),
                reaction.getUser().getAvatarUrl(),
                reaction.getEmoji(),
                reaction.getCreatedAt(),
                reaction.getUpdatedAt()
        );
    }
}
