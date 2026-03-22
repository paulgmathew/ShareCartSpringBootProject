package com.sharecart.sharecart.shoppinglist.controller;

import com.sharecart.sharecart.shoppinglist.dto.CreateListRequest;
import com.sharecart.sharecart.shoppinglist.dto.InviteRequest;
import com.sharecart.sharecart.shoppinglist.dto.ShoppingListResponse;
import com.sharecart.sharecart.shoppinglist.service.ShoppingListService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lists")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    // POST /api/v1/lists
    @PostMapping
    public ResponseEntity<ShoppingListResponse> createList(@Valid @RequestBody CreateListRequest request) {
        ShoppingListResponse created = shoppingListService.createList(request);
        URI location = URI.create("/api/v1/lists/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    // GET /api/v1/lists/{id}
    @GetMapping("/{id}")
    public ShoppingListResponse getListById(@PathVariable UUID id) {
        return shoppingListService.getListById(id);
    }

    // POST /api/v1/lists/{id}/invite
    @PostMapping("/{id}/invite")
    public ResponseEntity<Void> inviteUser(@PathVariable UUID id, @Valid @RequestBody InviteRequest request) {
        shoppingListService.inviteUser(id, request);
        return ResponseEntity.ok().build();
    }
}
