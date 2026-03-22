package com.sharecart.sharecart.item.controller;

import com.sharecart.sharecart.item.dto.CreateItemRequest;
import com.sharecart.sharecart.item.dto.ItemResponse;
import com.sharecart.sharecart.item.dto.UpdateItemRequest;
import com.sharecart.sharecart.item.service.ItemService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // POST /api/v1/lists/{listId}/items
    @PostMapping("/api/v1/lists/{listId}/items")
    public ResponseEntity<ItemResponse> addItem(
            @PathVariable UUID listId,
            @Valid @RequestBody CreateItemRequest request) {
        ItemResponse created = itemService.addItem(listId, request);
        URI location = URI.create("/api/v1/items/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    // PUT /api/v1/items/{id}
    @PutMapping("/api/v1/items/{id}")
    public ItemResponse updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateItemRequest request) {
        return itemService.updateItem(id, request);
    }

    // DELETE /api/v1/items/{id}
    @DeleteMapping("/api/v1/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
