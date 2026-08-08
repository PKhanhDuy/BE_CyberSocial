package com.cybersocial.auth.controller;

import com.cybersocial.auth.dto.AuthResponse;
import com.cybersocial.auth.dto.ChangePasswordRequest;
import com.cybersocial.auth.dto.ForgotPasswordRequest;
import com.cybersocial.auth.dto.LoginRequest;
import com.cybersocial.auth.dto.RegisterRequest;
import com.cybersocial.auth.dto.ResetPasswordRequest;
import com.cybersocial.auth.dto.ResetTokenValidationResponse;
import com.cybersocial.auth.service.AuthService;
import com.cybersocial.auth.service.AuthenticationResult;
import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "Refresh";

    private final AuthService authService;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
            AuthService authService,
            @Value("${app.security.cookie.secure:true}") boolean cookieSecure,
            @Value("${app.security.cookie.same-site:Lax}") String cookieSameSite
    ) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
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
        return ResponseEntity.ok(ApiResponse.success(
                "If this email is registered, a password reset link has been sent",
                null
        ));
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<ApiResponse<ResetTokenValidationResponse>> validateResetToken(
            @RequestParam("token") String token
    ) {
        authService.validateResetToken(token);
        return ResponseEntity.ok(ApiResponse.success("Reset token is valid", new ResetTokenValidationResponse(true)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
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
        ResponseCookie cookie = refreshCookieBuilder(result.refreshToken())
                .maxAge(Duration.ofSeconds(result.refreshTokenMaxAgeSeconds()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie expiredRefreshCookie() {
        return refreshCookieBuilder("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder refreshCookieBuilder(String value) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth");
    }
}
