package com.sharecart.sharecart.price.dto;

import jakarta.validation.constraints.NotBlank;

public record ComparePriceRequest(
        @NotBlank(message = "Item name is required") String itemName
) {
}
