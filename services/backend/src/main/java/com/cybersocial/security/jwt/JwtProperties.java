package com.cybersocial.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTokenMinutes,
        long refreshTokenDays,
        long refreshTokenSessionDays
) {
}
