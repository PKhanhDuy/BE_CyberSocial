package com.cybersocial.auth.controller;

import com.cybersocial.auth.dto.AuthResponse;
import com.cybersocial.auth.dto.ChangePasswordRequest;
import com.cybersocial.auth.dto.ForgotPasswordRequest;
import com.cybersocial.auth.dto.LoginRequest;
import com.cybersocial.auth.dto.RegisterRequest;
import com.cybersocial.auth.service.AuthService;
import com.cybersocial.auth.service.AuthenticationResult;
import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "Refresh";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthenticationResult result = authService.register(request);
        addRefreshCookie(response, result);
        return ResponseEntity.ok(ApiResponse.success("Registered successfully", result.response()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthenticationResult result = authService.login(request);
        addRefreshCookie(response, result);
        return ResponseEntity.ok(ApiResponse.success("Logged in successfully", result.response()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Temporary password has been sent to your registered email", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.requireCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME) String refreshToken,
            HttpServletResponse response
    ) {
        AuthenticationResult result = authService.refresh(refreshToken);
        addRefreshCookie(response, result);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    private void addRefreshCookie(HttpServletResponse response, AuthenticationResult result) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(result.refreshTokenMaxAgeSeconds()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
