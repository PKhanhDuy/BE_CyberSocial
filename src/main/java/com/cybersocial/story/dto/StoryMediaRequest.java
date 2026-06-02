package com.cybersocial.story.dto;

import com.cybersocial.story.StoryMediaType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StoryMediaRequest(
        @NotNull(message = "Media type is required")
        StoryMediaType mediaType,

        @NotBlank(message = "Media URL is required")
        @Size(max = 2048, message = "Media URL must be 2048 characters or fewer")
        String mediaUrl,

        @Size(max = 2048, message = "Thumbnail URL must be 2048 characters or fewer")
        String thumbnailUrl,

        @Min(value = 1, message = "Width must be positive")
        Integer width,

        @Min(value = 1, message = "Height must be positive")
        Integer height,

        @Positive(message = "Duration must be positive")
        Integer durationMs
) {
}
