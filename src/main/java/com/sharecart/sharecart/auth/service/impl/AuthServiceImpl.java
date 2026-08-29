package com.sharecart.sharecart.auth.service.impl;

import com.sharecart.sharecart.auth.dto.AuthResponse;
import com.sharecart.sharecart.auth.dto.LoginRequest;
import com.sharecart.sharecart.auth.dto.RegisterRequest;
import com.sharecart.sharecart.auth.dto.RegisterResponse;
import com.sharecart.sharecart.auth.dto.ResendVerificationRequest;
import com.sharecart.sharecart.auth.dto.VerifyEmailResponse;
import com.sharecart.sharecart.auth.service.AuthService;
import com.sharecart.sharecart.auth.service.VerificationEmailService;
import com.sharecart.sharecart.common.exception.InvalidCredentialsException;
import com.sharecart.sharecart.common.security.JwtUtil;
import com.sharecart.sharecart.user.model.User;
import com.sharecart.sharecart.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final VerificationEmailService verificationEmailService;

    @Value("${app.mail.verify-base-url}")
    private String verifyBaseUrl;

    @Value("${app.mail.verification-expiry-minutes:30}")
    private long verificationExpiryMinutes;

    @Override
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("Registration attempt with already-registered email");
            throw new IllegalStateException("Email is already registered: " + request.email());
        }

        String verificationToken = generateVerificationToken();
        LocalDateTime verificationExpiry = LocalDateTime.now().plusMinutes(verificationExpiryMinutes);

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .emailVerificationExpiresAt(verificationExpiry)
                .build();

        User saved = userRepository.save(user);

        String verificationLink = buildVerificationLink(verificationToken);
        verificationEmailService.sendVerificationEmail(saved.getEmail(), saved.getName(), verificationLink);
        log.info("User registered and verification email queued userId={}", saved.getId());

        return new RegisterResponse(
                "Registration successful. Please check your email to verify your account.",
                saved.getEmail(),
                saved.isEmailVerified()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // deliberately use a generic error to avoid revealing whether the email exists
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login attempt with unknown email");
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed — wrong password userId={}", user.getId());
            throw new InvalidCredentialsException();
        }

        if (!user.isEmailVerified()) {
            log.warn("Login blocked for unverified email userId={}", user.getId());
            throw new IllegalStateException("Email is not verified. Please verify your email before logging in.");
        }

        log.info("User logged in userId={}", user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(), user.getName());
    }

    @Override
    public VerifyEmailResponse verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Verification token is required");
        }

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        LocalDateTime expiry = user.getEmailVerificationExpiresAt();
        if (expiry == null || expiry.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Verification token has expired. Please request a new verification email.");
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);

        User saved = userRepository.save(user);
        log.info("Email verified userId={}", saved.getId());

        return new VerifyEmailResponse(
                "Email verified successfully. You can now log in.",
                saved.getEmail(),
                saved.isEmailVerified()
        );
    }

    @Override
    public VerifyEmailResponse resendVerificationEmail(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("No account found for email: " + request.email()));

        if (user.isEmailVerified()) {
            return new VerifyEmailResponse(
                    "Email is already verified.",
                    user.getEmail(),
                    true
            );
        }

        String verificationToken = generateVerificationToken();
        LocalDateTime verificationExpiry = LocalDateTime.now().plusMinutes(verificationExpiryMinutes);

        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationExpiresAt(verificationExpiry);

        User saved = userRepository.save(user);
        String verificationLink = buildVerificationLink(verificationToken);
        verificationEmailService.sendVerificationEmail(saved.getEmail(), saved.getName(), verificationLink);
        log.info("Verification email resent userId={}", saved.getId());

        return new VerifyEmailResponse(
                "Verification email sent. Please check your inbox.",
                saved.getEmail(),
                false
        );
    }

    private String generateVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String buildVerificationLink(String token) {
        if (verifyBaseUrl.contains("{token}")) {
            return verifyBaseUrl.replace("{token}", token);
        }
        return verifyBaseUrl + token;
    }
}
