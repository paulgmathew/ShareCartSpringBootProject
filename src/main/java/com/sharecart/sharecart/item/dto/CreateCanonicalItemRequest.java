package com.sharecart.sharecart.item.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCanonicalItemRequest(
        @NotBlank(message = "Item name is required") String name,
        String description
) {
}
