package com.sharecart.sharecart.item.service.impl;

import com.sharecart.sharecart.common.exception.ResourceNotFoundException;
import com.sharecart.sharecart.item.dto.CreateItemRequest;
import com.sharecart.sharecart.item.dto.ItemResponse;
import com.sharecart.sharecart.item.dto.UpdateItemRequest;
import com.sharecart.sharecart.item.model.Item;
import com.sharecart.sharecart.item.repository.ItemRepository;
import com.sharecart.sharecart.item.service.ItemService;
import com.sharecart.sharecart.realtime.service.ListRealtimePublisher;
import com.sharecart.sharecart.shoppinglist.model.ShoppingList;
import com.sharecart.sharecart.shoppinglist.repository.ShoppingListRepository;
import com.sharecart.sharecart.user.model.User;
import com.sharecart.sharecart.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final UserRepository userRepository;
    private final ListRealtimePublisher listRealtimePublisher;

    @Override
    @Transactional
    public ItemResponse addItem(UUID listId, CreateItemRequest request) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping list not found with id: " + listId));

        User createdBy = null;
        if (request.createdBy() != null) {
            createdBy = userRepository.findById(request.createdBy())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.createdBy()));
        }

        Item item = Item.builder()
                .shoppingList(shoppingList)
                .name(request.name())
                .quantity(request.quantity())
                .category(request.category())
                .createdBy(createdBy)
                .isCompleted(false)
                .build();

        ItemResponse created = toResponse(itemRepository.save(item));
        listRealtimePublisher.publishItemAdded(created.listId(), created);
        return created;
    }

    @Override
    @Transactional
    public ItemResponse updateItem(UUID id, UpdateItemRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (request.name() != null) item.setName(request.name());
        if (request.quantity() != null) item.setQuantity(request.quantity());
        if (request.isCompleted() != null) item.setIsCompleted(request.isCompleted());
        if (request.category() != null) item.setCategory(request.category());

        ItemResponse updated = toResponse(itemRepository.save(item));
        listRealtimePublisher.publishItemUpdated(updated.listId(), updated);
        return updated;
    }

    @Override
    @Transactional
    public void deleteItem(UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        ItemResponse deleted = toResponse(item);
        itemRepository.delete(item);
        listRealtimePublisher.publishItemDeleted(deleted.listId(), deleted);
    }

    private ItemResponse toResponse(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getShoppingList().getId(),
                item.getName(),
                item.getQuantity(),
                item.getIsCompleted(),
                item.getCategory(),
                item.getCreatedBy() != null ? item.getCreatedBy().getId() : null,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
