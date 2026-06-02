package com.cybersocial.story.dto;

import com.cybersocial.story.StoryView;
import java.time.Instant;
import java.util.UUID;

public record StoryViewerResponse(
        UUID userId,
        String displayName,
        String avatarUrl,
        Instant viewedAt
) {
    public static StoryViewerResponse from(StoryView view) {
        return new StoryViewerResponse(
                view.getViewer().getId(),
                view.getViewer().getDisplayName(),
                view.getViewer().getAvatarUrl(),
                view.getViewedAt()
        );
    }
}
