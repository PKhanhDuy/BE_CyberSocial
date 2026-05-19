package com.cybersocial.user;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.util.SecurityUtils;
import com.cybersocial.user.dto.ThemeResponse;
import com.cybersocial.user.dto.UpdateThemeRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/theme")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ThemeResponse>> getTheme() {
        return ResponseEntity.ok(ApiResponse.success(themeService.getTheme(SecurityUtils.requireCurrentUserId())));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ThemeResponse>> updateTheme(@Valid @RequestBody UpdateThemeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Theme updated",
                themeService.updateTheme(SecurityUtils.requireCurrentUserId(), request.theme())
        ));
    }
}
