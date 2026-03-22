package com.sharecart.sharecart.item.dto;

public record UpdateItemRequest(
        String name,
        String quantity,
        Boolean isCompleted,
        String category
) {
}
