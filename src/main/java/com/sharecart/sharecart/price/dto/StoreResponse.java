package com.sharecart.sharecart.price.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StoreResponse(
        UUID id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        LocalDateTime createdAt
) {
}
