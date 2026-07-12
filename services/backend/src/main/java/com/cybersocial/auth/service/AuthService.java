package com.cybersocial.auth.service;

import com.cybersocial.auth.dto.ChangePasswordRequest;
import com.cybersocial.auth.dto.ForgotPasswordRequest;
import com.cybersocial.auth.dto.LoginRequest;
import com.cybersocial.auth.dto.RegisterRequest;
import com.cybersocial.auth.dto.ResetPasswordRequest;
import java.util.UUID;

public interface AuthService {

    AuthenticationResult register(RegisterRequest request);

    AuthenticationResult login(LoginRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void validateResetToken(String token);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);

    AuthenticationResult refresh(String refreshToken);

    void logout(String refreshToken);
}
