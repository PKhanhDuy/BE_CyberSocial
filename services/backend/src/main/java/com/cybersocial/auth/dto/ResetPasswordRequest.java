package com.cybersocial.auth.dto;

import com.cybersocial.auth.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required")
        String token,

        @NotBlank(message = "New password is required")
        @StrongPassword
        String newPassword
) {
}
