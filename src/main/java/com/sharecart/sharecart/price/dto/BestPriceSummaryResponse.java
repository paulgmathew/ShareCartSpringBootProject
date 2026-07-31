package com.sharecart.sharecart.price.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BestPriceSummaryResponse(
        UUID canonicalItemId,
        String itemName,
        BigDecimal lowestPrice,
        UUID storeId,
        String storeName
) {
}