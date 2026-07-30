package com.sharecart.sharecart.price.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StoreInfoRequest(
        @NotBlank(message = "Store name is required") String name,
        String address,
        @NotNull(message = "Store latitude is required") Double latitude,
        @NotNull(message = "Store longitude is required") Double longitude
) {
}