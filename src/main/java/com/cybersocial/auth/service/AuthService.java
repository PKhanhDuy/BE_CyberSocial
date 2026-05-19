package com.cybersocial.auth.service;

import com.cybersocial.auth.dto.LoginRequest;
import com.cybersocial.auth.dto.RegisterRequest;

public interface AuthService {

    AuthenticationResult register(RegisterRequest request);

    AuthenticationResult login(LoginRequest request);

    AuthenticationResult refresh(String refreshToken);

    void logout(String refreshToken);
}
