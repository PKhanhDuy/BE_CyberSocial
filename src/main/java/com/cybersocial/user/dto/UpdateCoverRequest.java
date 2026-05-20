package com.cybersocial.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCoverRequest(
        @NotBlank(message = "Cover URL is required")
        @Size(max = 2048, message = "Cover URL must be 2048 characters or fewer")
        String coverUrl
) {
}
