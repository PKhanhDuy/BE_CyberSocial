package com.cybersocial.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAvatarRequest(
        @NotBlank(message = "Avatar URL is required")
        @Size(max = 2048, message = "Avatar URL must be 2048 characters or fewer")
        String avatarUrl
) {
}
