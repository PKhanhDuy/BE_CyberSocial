package com.cybersocial.auth.dto;

import com.cybersocial.auth.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Display name is required")
        @Size(min = 2, max = 120, message = "Display name must be between 2 and 120 characters")
        String displayName,

        @NotBlank(message = "Password is required")
        @StrongPassword
        String password
) {
}
