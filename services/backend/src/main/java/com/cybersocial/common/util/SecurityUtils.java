package com.cybersocial.common.util;

import com.cybersocial.security.service.AuthenticatedUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UUID> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return Optional.empty();
        }
        return Optional.of(user.getId());
    }

    public static UUID requireCurrentUserId() {
        return currentUserId().orElseThrow(() -> new IllegalStateException("Authenticated user is unavailable"));
    }
}
