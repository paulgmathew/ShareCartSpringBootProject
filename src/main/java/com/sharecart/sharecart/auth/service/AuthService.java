package com.sharecart.sharecart.auth.service;

import com.sharecart.sharecart.auth.dto.AuthResponse;
import com.sharecart.sharecart.auth.dto.LoginRequest;
import com.sharecart.sharecart.auth.dto.RegisterRequest;
import com.sharecart.sharecart.auth.dto.RegisterResponse;
import com.sharecart.sharecart.auth.dto.ResendVerificationRequest;
import com.sharecart.sharecart.auth.dto.VerifyEmailResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    VerifyEmailResponse verifyEmail(String token);

    VerifyEmailResponse resendVerificationEmail(ResendVerificationRequest request);
}
