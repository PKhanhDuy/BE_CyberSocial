package com.cybersocial.story.dto;

import com.cybersocial.story.StoryMedia;
import com.cybersocial.story.StoryMediaType;
import java.util.UUID;

public record StoryMediaResponse(
        UUID id,
        StoryMediaType mediaType,
        String mediaUrl,
        String thumbnailUrl,
        Integer width,
        Integer height,
        Integer durationMs
) {
    public static StoryMediaResponse from(StoryMedia media) {
        if (media == null) {
            return null;
        }
        return new StoryMediaResponse(
                media.getId(),
                media.getMediaType(),
                media.getMediaUrl(),
                media.getThumbnailUrl(),
                media.getWidth(),
                media.getHeight(),
                media.getDurationMs()
        );
    }
}
