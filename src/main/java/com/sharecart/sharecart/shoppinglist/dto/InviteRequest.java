package com.sharecart.sharecart.shoppinglist.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InviteRequest(

        @NotNull(message = "User ID is required")
        UUID userId,

        String role
) {
}
