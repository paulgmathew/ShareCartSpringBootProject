package com.sharecart.sharecart.auth.dto;

public record RegisterResponse(
        String message,
        String email,
        boolean emailVerified
) {}
