package com.sharecart.sharecart.shoppinglist.dto;

import com.sharecart.sharecart.item.dto.ItemResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ShoppingListResponse(
        UUID id,
        String name,
        UUID ownerId,
        String ownerName,
        List<ItemResponse> items,
        List<MemberResponse> members,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
