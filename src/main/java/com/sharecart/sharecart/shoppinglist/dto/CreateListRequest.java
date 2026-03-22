package com.sharecart.sharecart.shoppinglist.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateListRequest(

        @NotBlank(message = "List name is required")
        String name,

        UUID ownerId
) {
}
