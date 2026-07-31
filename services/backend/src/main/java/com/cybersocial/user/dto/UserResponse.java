package com.cybersocial.user.dto;

import com.cybersocial.user.ThemePreference;
import com.cybersocial.user.User;
import com.cybersocial.user.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        String coverUrl,
        UserRole role,
        boolean enabled,
        ThemePreference themePreference,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getCoverUrl(),
                user.getRole(),
                user.isEnabled(),
                user.getThemePreference(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
