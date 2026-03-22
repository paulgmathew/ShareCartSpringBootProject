package com.sharecart.sharecart.item.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateItemRequest(

        @NotBlank(message = "Item name is required")
        String name,

        String quantity,

        String category,

        UUID createdBy
) {
}
