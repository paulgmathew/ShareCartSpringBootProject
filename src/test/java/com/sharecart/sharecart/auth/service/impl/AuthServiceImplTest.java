package com.sharecart.sharecart.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sharecart.sharecart.auth.dto.AuthResponse;
import com.sharecart.sharecart.auth.dto.LoginRequest;
import com.sharecart.sharecart.auth.dto.RegisterRequest;
import com.sharecart.sharecart.auth.dto.RegisterResponse;
import com.sharecart.sharecart.auth.dto.ResendVerificationRequest;
import com.sharecart.sharecart.auth.dto.VerifyEmailResponse;
import com.sharecart.sharecart.auth.service.VerificationEmailService;
import com.sharecart.sharecart.common.exception.InvalidCredentialsException;
import com.sharecart.sharecart.common.security.JwtUtil;
import com.sharecart.sharecart.user.model.User;
import com.sharecart.sharecart.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private VerificationEmailService verificationEmailService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtUtil, verificationEmailService);
        ReflectionTestUtils.setField(authService, "verifyBaseUrl", "http://localhost:8080/api/v1/auth/verify-email?token=");
        ReflectionTestUtils.setField(authService, "verificationExpiryMinutes", 30L);
    }

    @Test
    void shouldRegisterUnverifiedUserAndSendVerificationEmail() {
        RegisterRequest request = new RegisterRequest("paul@example.com", "password123", "Paul");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        RegisterResponse response = authService.register(request);

        assertEquals("paul@example.com", response.email());
        assertFalse(response.emailVerified());
        verify(jwtUtil, never()).generateToken(any(UUID.class), anyString());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertFalse(saved.isEmailVerified());
        assertNotNull(saved.getEmailVerificationToken());
        assertNotNull(saved.getEmailVerificationExpiresAt());

        verify(verificationEmailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldBlockLoginWhenEmailNotVerified() {
        LoginRequest request = new LoginRequest("paul@example.com", "password123");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("paul@example.com")
                .passwordHash("encoded-password")
                .emailVerified(false)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> authService.login(request));

        assertTrue(ex.getMessage().contains("Email is not verified"));
        verify(jwtUtil, never()).generateToken(any(UUID.class), anyString());
    }

    @Test
    void shouldLoginWhenEmailVerified() {
        LoginRequest request = new LoginRequest("paul@example.com", "password123");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("paul@example.com")
                .name("Paul")
                .passwordHash("encoded-password")
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(user.getId(), user.getEmail())).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.token());
        assertEquals(user.getId(), response.userId());
        assertEquals("paul@example.com", response.email());
    }

    @Test
    void shouldVerifyEmailWithValidToken() {
        String token = "valid-token";
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("paul@example.com")
                .emailVerified(false)
                .emailVerificationToken(token)
                .emailVerificationExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userRepository.findByEmailVerificationToken(token)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerifyEmailResponse response = authService.verifyEmail(token);

        assertTrue(response.emailVerified());
        assertEquals("paul@example.com", response.email());
        assertTrue(user.isEmailVerified());
        assertNull(user.getEmailVerificationToken());
        assertNull(user.getEmailVerificationExpiresAt());
        assertNotNull(user.getEmailVerifiedAt());
    }

    @Test
    void shouldRejectExpiredVerificationToken() {
        String token = "expired-token";
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("paul@example.com")
                .emailVerified(false)
                .emailVerificationToken(token)
                .emailVerificationExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(userRepository.findByEmailVerificationToken(token)).thenReturn(Optional.of(user));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> authService.verifyEmail(token));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void shouldResendVerificationEmailForUnverifiedUser() {
        ResendVerificationRequest request = new ResendVerificationRequest("paul@example.com");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("paul@example.com")
                .name("Paul")
                .emailVerified(false)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerifyEmailResponse response = authService.resendVerificationEmail(request);

        assertFalse(response.emailVerified());
        assertNotNull(user.getEmailVerificationToken());
        assertNotNull(user.getEmailVerificationExpiresAt());
        verify(verificationEmailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectInvalidCredentials() {
        LoginRequest request = new LoginRequest("paul@example.com", "password123");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}
