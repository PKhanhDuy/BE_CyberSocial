package com.cybersocial.user;

import com.cybersocial.user.dto.ThemeResponse;
import java.util.UUID;

public interface ThemeService {

    ThemeResponse getTheme(UUID currentUserId);

    ThemeResponse updateTheme(UUID currentUserId, ThemePreference theme);
}
