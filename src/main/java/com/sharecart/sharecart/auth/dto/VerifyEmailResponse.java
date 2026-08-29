package com.sharecart.sharecart.auth.dto;

public record VerifyEmailResponse(
        String message,
        String email,
        boolean emailVerified
) {}
