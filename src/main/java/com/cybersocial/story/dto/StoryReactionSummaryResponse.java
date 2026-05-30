package com.cybersocial.story.dto;

import com.cybersocial.story.StoryReaction;
import java.time.Instant;
import java.util.UUID;

public record StoryReactionSummaryResponse(
        UUID userId,
        String displayName,
        String avatarUrl,
        String reactionType,
        Instant createdAt
) {
    public static StoryReactionSummaryResponse from(StoryReaction reaction) {
        return new StoryReactionSummaryResponse(
                reaction.getUser().getId(),
                reaction.getUser().getDisplayName(),
                reaction.getUser().getAvatarUrl(),
                reaction.getReactionType(),
                reaction.getCreatedAt()
        );
    }
}
