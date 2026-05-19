package com.cybersocial.auth.dto;

import com.cybersocial.user.dto.UserResponse;

public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        UserResponse user
) {
}
