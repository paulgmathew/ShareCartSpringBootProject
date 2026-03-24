package com.sharecart.sharecart.auth.service.impl;

import com.sharecart.sharecart.auth.dto.AuthResponse;
import com.sharecart.sharecart.auth.dto.LoginRequest;
import com.sharecart.sharecart.auth.dto.RegisterRequest;
import com.sharecart.sharecart.auth.service.AuthService;
import com.sharecart.sharecart.common.exception.InvalidCredentialsException;
import com.sharecart.sharecart.common.security.JwtUtil;
import com.sharecart.sharecart.user.model.User;
import com.sharecart.sharecart.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("Email is already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();

        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getId(), saved.getEmail());

        return new AuthResponse(token, "Bearer", saved.getId(), saved.getEmail(), saved.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // deliberately use a generic error to avoid revealing whether the email exists
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(), user.getName());
    }
}
