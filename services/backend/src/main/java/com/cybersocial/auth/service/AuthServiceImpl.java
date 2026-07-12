package com.cybersocial.auth.service;

import com.cybersocial.auth.PasswordResetToken;
import com.cybersocial.auth.RefreshToken;
import com.cybersocial.auth.dto.AuthResponse;
import com.cybersocial.auth.dto.ChangePasswordRequest;
import com.cybersocial.auth.dto.ForgotPasswordRequest;
import com.cybersocial.auth.dto.LoginRequest;
import com.cybersocial.auth.dto.RegisterRequest;
import com.cybersocial.auth.dto.ResetPasswordRequest;
import com.cybersocial.auth.repository.PasswordResetTokenRepository;
import com.cybersocial.auth.repository.RefreshTokenRepository;
import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ConflictException;
import com.cybersocial.common.util.HashUtil;
import com.cybersocial.security.jwt.JwtUtil;
import com.cybersocial.user.User;
import com.cybersocial.user.UserRole;
import com.cybersocial.user.dto.UserResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;
    private final String resetPasswordFromAddress;
    private final String frontendBaseUrl;
    private final long passwordResetExpiresMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String resetPasswordFromAddress,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${app.password-reset.expires-minutes:30}") long passwordResetExpiresMinutes
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.mailSender = mailSender;
        this.resetPasswordFromAddress = resetPasswordFromAddress;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.passwordResetExpiresMinutes = passwordResetExpiresMinutes;
    }

    @Override
    @Transactional
    public AuthenticationResult register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email is already registered");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .displayName(request.displayName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .build();

        return issueTokens(userRepository.save(user));
    }

    @Override
    @Transactional
    public AuthenticationResult login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException exception) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return issueTokens(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            Instant now = Instant.now();
            passwordResetTokenRepository.invalidateActiveTokensForUser(user.getId(), now);

            String resetToken = newResetToken();
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(HashUtil.sha256(resetToken))
                    .expiresAt(now.plus(passwordResetExpiresMinutes, ChronoUnit.MINUTES))
                    .build();
            passwordResetTokenRepository.save(token);
            sendResetLink(user, resetToken);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public void validateResetToken(String token) {
        findActiveResetToken(token);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = findActiveResetToken(request.token());
        User user = resetToken.getUser();

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        Instant now = Instant.now();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsedAt(now);
        passwordResetTokenRepository.invalidateActiveTokensForUser(user.getId(), now);
        refreshTokenRepository.revokeActiveTokensForUser(user.getId(), now);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Authenticated user is unavailable"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Override
    @Transactional
    public AuthenticationResult refresh(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(HashUtil.sha256(refreshToken))
                .orElseThrow(() -> new BadRequestException("Refresh token is invalid"));

        if (!storedToken.isActive()) {
            throw new BadRequestException("Refresh token is expired or revoked");
        }

        storedToken.setRevokedAt(Instant.now());
        return issueTokens(storedToken.getUser());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(HashUtil.sha256(refreshToken))
                .filter(RefreshToken::isActive)
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    private PasswordResetToken findActiveResetToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Reset token is invalid or expired");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(HashUtil.sha256(token))
                .orElseThrow(() -> new BadRequestException("Reset token is invalid or expired"));

        if (!resetToken.isActive()) {
            throw new BadRequestException("Reset token is invalid or expired");
        }

        return resetToken;
    }

    private AuthenticationResult issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshTokenValue = newRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(HashUtil.sha256(refreshTokenValue))
                .expiresAt(jwtUtil.refreshTokenExpiry())
                .build();
        refreshTokenRepository.save(refreshToken);

        AuthResponse response = new AuthResponse(
                "Bearer",
                accessToken,
                jwtUtil.accessTokenTtlSeconds(),
                UserResponse.from(user)
        );
        return new AuthenticationResult(response, refreshTokenValue, jwtUtil.refreshTokenTtlSeconds());
    }

    private String newRefreshToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String newResetToken() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void sendResetLink(User user, String resetToken) {
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + resetToken;
        SimpleMailMessage message = new SimpleMailMessage();
        if (resetPasswordFromAddress != null && !resetPasswordFromAddress.isBlank()) {
            message.setFrom(resetPasswordFromAddress);
        }
        message.setTo(user.getEmail());
        message.setSubject("CyberSocial - Reset your password");
        message.setText("""
                Hello %s,

                We received a request to reset your CyberSocial password.
                Open the link below to choose a new password:

                %s

                This link expires in %d minutes and can only be used once.
                If you did not request this, you can safely ignore this email.

                CyberSocial
                """.formatted(user.getDisplayName(), resetUrl, passwordResetExpiresMinutes));
        mailSender.send(message);
    }
}
