package com.sharecart.service;

import com.sharecart.dto.CreateItemRequest;
import com.sharecart.dto.UpdateItemRequest;
import com.sharecart.entity.Item;
import com.sharecart.entity.ShoppingList;
import com.sharecart.exception.ResourceNotFoundException;
import com.sharecart.repository.ItemRepository;
import com.sharecart.repository.ShoppingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ShoppingListRepository shoppingListRepository;

    public ItemService(ItemRepository itemRepository,
                       ShoppingListRepository shoppingListRepository) {
        this.itemRepository = itemRepository;
        this.shoppingListRepository = shoppingListRepository;
    }

    @Transactional
    public Item addItem(Long listId, CreateItemRequest request) {
        ShoppingList list = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shopping list not found with id: " + listId));
        Item item = new Item(request.getName(), request.getQuantity(), list);
        return itemRepository.save(item);
    }

    @Transactional
    public Item updateItem(Long itemId, UpdateItemRequest request) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item not found with id: " + itemId));
        if (request.getName() != null) {
            item.setName(request.getName());
        }
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getChecked() != null) {
            item.setChecked(request.getChecked());
        }
        return itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item not found with id: " + itemId));
        itemRepository.delete(item);
    }
}
