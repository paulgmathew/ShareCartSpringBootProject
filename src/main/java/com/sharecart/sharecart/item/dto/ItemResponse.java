package com.sharecart.sharecart.item.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        UUID listId,
        String name,
        String quantity,
        Boolean isCompleted,
        String category,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
