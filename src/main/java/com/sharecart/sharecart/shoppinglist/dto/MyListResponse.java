package com.sharecart.sharecart.shoppinglist.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MyListResponse(
        UUID id,
        String name,
        UUID ownerId,
        String ownerName,
        String memberRole,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}