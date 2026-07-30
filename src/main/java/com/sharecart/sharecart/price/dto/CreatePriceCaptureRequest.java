package com.sharecart.sharecart.price.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePriceCaptureRequest(
        String rawText,
        String imageUrl,
        @NotNull(message = "Latitude is required") Double latitude,
        @NotNull(message = "Longitude is required") Double longitude
) {
}
