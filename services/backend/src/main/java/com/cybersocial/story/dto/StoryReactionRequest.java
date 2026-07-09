package com.cybersocial.story.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoryReactionRequest(
        @NotBlank(message = "Reaction type is required")
        @Size(max = 40, message = "Reaction type must be 40 characters or fewer")
        String reactionType
) {
}
