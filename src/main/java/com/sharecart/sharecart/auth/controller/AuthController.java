package com.sharecart.sharecart.auth.controller;

import com.sharecart.sharecart.auth.dto.AuthResponse;
import com.sharecart.sharecart.auth.dto.LoginRequest;
import com.sharecart.sharecart.auth.dto.RegisterRequest;
import com.sharecart.sharecart.auth.dto.RegisterResponse;
import com.sharecart.sharecart.auth.dto.ResendVerificationRequest;
import com.sharecart.sharecart.auth.dto.VerifyEmailResponse;
import com.sharecart.sharecart.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/v1/auth/register
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/v1/auth/register");
        RegisterResponse response = authService.register(request);
        log.info("Registration successful email={}", response.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/v1/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/v1/auth/login");
        AuthResponse response = authService.login(request);
        log.info("Login successful userId={}", response.userId());
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/auth/verify-email?token=...
    @GetMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@RequestParam("token") String token) {
        log.info("GET /api/v1/auth/verify-email");
        VerifyEmailResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    // POST /api/v1/auth/resend-verification
    @PostMapping("/resend-verification")
    public ResponseEntity<VerifyEmailResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        log.info("POST /api/v1/auth/resend-verification");
        VerifyEmailResponse response = authService.resendVerificationEmail(request);
        return ResponseEntity.ok(response);
    }
}
