package com.sharecart.sharecart.realtime.dto;

import com.sharecart.sharecart.item.dto.ItemResponse;
import java.time.Instant;
import java.util.UUID;

public record ListItemRealtimeEvent(
        String eventType,
        UUID listId,
        ItemResponse item,
        Instant occurredAt
) {
}