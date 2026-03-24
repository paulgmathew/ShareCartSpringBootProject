package com.sharecart.sharecart.shoppinglist.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateListRequest(

        @NotBlank(message = "List name is required")
        String name
) {
}
