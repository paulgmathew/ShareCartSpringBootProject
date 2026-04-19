package com.sharecart.sharecart.realtime.service;

import com.sharecart.sharecart.item.dto.ItemResponse;
import java.util.UUID;

public interface ListRealtimePublisher {

    void publishItemAdded(UUID listId, ItemResponse item);

    void publishItemUpdated(UUID listId, ItemResponse item);

    void publishItemDeleted(UUID listId, ItemResponse item);
}