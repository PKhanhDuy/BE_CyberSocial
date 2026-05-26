package com.cybersocial.auth.service;

import com.cybersocial.auth.RefreshToken;
import com.cybersocial.auth.dto.AuthResponse;
import com.cybersocial.auth.dto.ForgotPasswordRequest;
import com.cybersocial.auth.dto.LoginRequest;
import com.cybersocial.auth.dto.RegisterRequest;
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
import java.util.Base64;
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
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;
    private final String resetPasswordFromAddress;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String resetPasswordFromAddress
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.mailSender = mailSender;
        this.resetPasswordFromAddress = resetPasswordFromAddress;
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
            String temporaryPassword = newTemporaryPassword();
            user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
            refreshTokenRepository.revokeActiveTokensForUser(user.getId(), Instant.now());
            sendTemporaryPassword(user, temporaryPassword);
        });
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

    private String newTemporaryPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }
        return password.toString();
    }

    private void sendTemporaryPassword(User user, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (resetPasswordFromAddress != null && !resetPasswordFromAddress.isBlank()) {
            message.setFrom(resetPasswordFromAddress);
        }
        message.setTo(user.getEmail());
        message.setSubject("CyberSocial - Temporary password");
        message.setText("""
                Hello %s,

                Your CyberSocial password has been reset.
                Temporary password: %s

                Please sign in and change this password immediately.

                CyberSocial
                """.formatted(user.getDisplayName(), temporaryPassword));
        mailSender.send(message);
    }
}
