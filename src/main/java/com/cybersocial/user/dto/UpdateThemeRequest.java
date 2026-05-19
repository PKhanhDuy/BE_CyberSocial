package com.cybersocial.user.dto;

import com.cybersocial.user.ThemePreference;
import jakarta.validation.constraints.NotNull;

public record UpdateThemeRequest(
        @NotNull(message = "Theme is required")
        ThemePreference theme
) {
}
