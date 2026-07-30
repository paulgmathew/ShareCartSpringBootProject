package com.sharecart.sharecart.price.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ConfirmPriceItemRequest(
        @NotBlank(message = "Item name is required") String itemName,
        @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be greater than zero") BigDecimal price,
        String unit
) {
}