package com.cybersocial.user;

import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.user.dto.ThemeResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThemeServiceImpl implements ThemeService {

    private final UserRepository userRepository;

    public ThemeServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ThemeResponse getTheme(UUID currentUserId) {
        return new ThemeResponse(getUser(currentUserId).getThemePreference());
    }

    @Override
    @Transactional
    public ThemeResponse updateTheme(UUID currentUserId, ThemePreference theme) {
        User user = getUser(currentUserId);
        user.setThemePreference(theme);
        return new ThemeResponse(user.getThemePreference());
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
