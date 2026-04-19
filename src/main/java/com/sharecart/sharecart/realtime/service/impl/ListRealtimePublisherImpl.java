package com.sharecart.sharecart.realtime.service.impl;

import com.sharecart.sharecart.item.dto.ItemResponse;
import com.sharecart.sharecart.realtime.dto.ListItemRealtimeEvent;
import com.sharecart.sharecart.realtime.service.ListRealtimePublisher;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class ListRealtimePublisherImpl implements ListRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishItemAdded(UUID listId, ItemResponse item) {
        publishAfterCommit(listId, new ListItemRealtimeEvent("ITEM_ADDED", listId, item, Instant.now()));
    }

    @Override
    public void publishItemUpdated(UUID listId, ItemResponse item) {
        publishAfterCommit(listId, new ListItemRealtimeEvent("ITEM_UPDATED", listId, item, Instant.now()));
    }

    @Override
    public void publishItemDeleted(UUID listId, ItemResponse item) {
        publishAfterCommit(listId, new ListItemRealtimeEvent("ITEM_DELETED", listId, item, Instant.now()));
    }

    private void publishAfterCommit(UUID listId, ListItemRealtimeEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(listId, event);
                }
            });
            return;
        }

        send(listId, event);
    }

    private void send(UUID listId, ListItemRealtimeEvent event) {
        messagingTemplate.convertAndSend("/topic/lists/" + listId, event);
    }
}