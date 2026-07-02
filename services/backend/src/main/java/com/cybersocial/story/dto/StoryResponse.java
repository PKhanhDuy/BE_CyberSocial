package com.cybersocial.story.dto;

import com.cybersocial.story.Story;
import com.cybersocial.story.StoryVisibility;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoryResponse(
        UUID id,
        StoryAuthorResponse author,
        String caption,
        StoryVisibility visibility,
        StoryMediaResponse media,
        MusicTrackResponse music,
        Integer musicStartMs,
        Integer musicDurationMs,
        long viewCount,
        long reactionCount,
        boolean viewedByCurrentUser,
        String currentUserReaction,
        List<StoryViewerResponse> viewers,
        List<StoryReactionSummaryResponse> reactions,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static StoryResponse from(
            Story story,
            long viewCount,
            long reactionCount,
            boolean viewedByCurrentUser,
            String currentUserReaction,
            List<StoryViewerResponse> viewers,
            List<StoryReactionSummaryResponse> reactions
    ) {
        return new StoryResponse(
                story.getId(),
                StoryAuthorResponse.from(story.getAuthor()),
                story.getCaption(),
                story.getVisibility(),
                StoryMediaResponse.from(story.getMedia()),
                MusicTrackResponse.from(story.getMusicTrack()),
                story.getMusicStartMs(),
                story.getMusicDurationMs(),
                viewCount,
                reactionCount,
                viewedByCurrentUser,
                currentUserReaction,
                viewers,
                reactions,
                story.getExpiresAt(),
                story.getCreatedAt(),
                story.getUpdatedAt()
        );
    }
}
