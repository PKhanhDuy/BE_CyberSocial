package com.cybersocial.story.dto;

import com.cybersocial.story.StoryReaction;
import java.time.Instant;
import java.util.UUID;

public record StoryReactionResponse(
        UUID id,
        UUID storyId,
        UUID userId,
        String reactionType,
        Instant createdAt
) {
    public static StoryReactionResponse from(StoryReaction reaction) {
        return new StoryReactionResponse(
                reaction.getId(),
                reaction.getStory().getId(),
                reaction.getUser().getId(),
                reaction.getReactionType(),
                reaction.getCreatedAt()
        );
    }
}
