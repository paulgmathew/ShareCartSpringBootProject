package com.sharecart.sharecart.price.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ComparePriceResponse(
        BigDecimal lowestPrice,
        UUID lowestStoreId,
        String lowestStoreName,
        BigDecimal averagePrice,
        long totalEntries
) {
}
