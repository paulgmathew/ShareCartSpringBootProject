package com.sharecart.sharecart.price.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStoreRequest(
        @NotBlank(message = "Store name is required") String name,
        String address,
        @NotNull(message = "Latitude is required") Double latitude,
        @NotNull(message = "Longitude is required") Double longitude
) {
}
