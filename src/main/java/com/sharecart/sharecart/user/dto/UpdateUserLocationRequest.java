package com.sharecart.sharecart.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateUserLocationRequest(
        @NotNull(message = "Latitude is required") @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double latitude,
        @NotNull(message = "Longitude is required") @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double longitude
) {
}
