package com.sharecart.sharecart.price.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StorePriceResponse(
        UUID storeId,
        String storeName,
        BigDecimal price
) {
}
