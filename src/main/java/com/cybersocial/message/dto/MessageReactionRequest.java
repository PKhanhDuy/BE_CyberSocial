package com.cybersocial.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageReactionRequest(
        @NotBlank(message = "Emoji is required")
        @Size(max = 40, message = "Emoji must be 40 characters or fewer")
        String emoji
) {
}
