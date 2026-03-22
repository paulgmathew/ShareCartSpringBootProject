package com.sharecart.controller;

import com.sharecart.dto.CreateItemRequest;
import com.sharecart.dto.UpdateItemRequest;
import com.sharecart.entity.Item;
import com.sharecart.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping("/lists/{id}/items")
    public ResponseEntity<Item> addItem(@PathVariable Long id,
                                        @Valid @RequestBody CreateItemRequest request) {
        Item item = itemService.addItem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id,
                                           @Valid @RequestBody UpdateItemRequest request) {
        Item item = itemService.updateItem(id, request);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
