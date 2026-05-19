package com.cybersocial.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Display name is required")
        @Size(min = 2, max = 120, message = "Display name must be between 2 and 120 characters")
        String displayName
) {
}
