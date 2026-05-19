package com.cybersocial.auth.service;

import com.cybersocial.auth.dto.AuthResponse;

public record AuthenticationResult(
        AuthResponse response,
        String refreshToken,
        long refreshTokenMaxAgeSeconds
) {
}
