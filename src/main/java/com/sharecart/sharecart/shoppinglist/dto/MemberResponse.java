package com.sharecart.sharecart.shoppinglist.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberResponse(
        UUID userId,
        String name,
        String email,
        String role,
        LocalDateTime joinedAt
) {
}
