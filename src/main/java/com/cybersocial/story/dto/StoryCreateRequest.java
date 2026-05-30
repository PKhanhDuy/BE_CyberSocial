package com.cybersocial.story.dto;

import com.cybersocial.story.StoryVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StoryCreateRequest(
        @Size(max = 1000, message = "Caption must be 1000 characters or fewer")
        String caption,

        StoryVisibility visibility,

        @NotNull(message = "Story media is required")
        @Valid
        StoryMediaRequest media,

        UUID musicTrackId,

        @Min(value = 0, message = "Music start must be zero or greater")
        Integer musicStartMs,

        @Positive(message = "Music duration must be positive")
        Integer musicDurationMs
) {
}
