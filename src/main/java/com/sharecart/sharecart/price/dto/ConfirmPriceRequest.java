package com.sharecart.sharecart.price.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ConfirmPriceRequest(
        @NotNull(message = "Capture ID is required") UUID captureId,
        @NotBlank(message = "Item name is required") String itemName,
        @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be greater than zero") BigDecimal price,
        String unit,
        @NotBlank(message = "Store name is required") String storeName,
        @NotNull(message = "Latitude is required") Double latitude,
        @NotNull(message = "Longitude is required") Double longitude
) {
}
