package com.sharecart.sharecart.price.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ConfirmPriceRequest(
        @NotNull(message = "Capture ID is required") UUID captureId,
        @NotBlank(message = "Scan type is required") String scanType,
        @NotBlank(message = "Source is required") String source,
        @NotNull(message = "Captured at is required") OffsetDateTime capturedAt,
        @NotNull(message = "Store details are required") @Valid StoreInfoRequest store,
        @NotNull(message = "Items are required") @Size(min = 1, message = "At least one confirmed item is required") @Valid List<ConfirmPriceItemRequest> items
) {
}
