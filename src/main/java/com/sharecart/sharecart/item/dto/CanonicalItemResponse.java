package com.sharecart.sharecart.item.dto;

import java.util.UUID;

public record CanonicalItemResponse(
        UUID id,
        String name,
        String normalizedName,
        String description
) {
}
