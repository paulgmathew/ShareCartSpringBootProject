package com.sharecart.sharecart.price.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ItemPriceResponse(
        UUID id,
        String itemName,
        String normalizedName,
        UUID storeId,
        String storeName,
        BigDecimal price,
        String unit,
        LocalDateTime capturedAt,
        String source,
        UUID createdBy,
        LocalDateTime createdAt
) {
}
