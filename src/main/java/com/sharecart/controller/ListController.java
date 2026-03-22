package com.sharecart.controller;

import com.sharecart.dto.CreateListRequest;
import com.sharecart.dto.InviteRequest;
import com.sharecart.entity.ListMember;
import com.sharecart.entity.ShoppingList;
import com.sharecart.service.ShoppingListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lists")
public class ListController {

    private final ShoppingListService shoppingListService;

    public ListController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    @PostMapping
    public ResponseEntity<ShoppingList> createList(@Valid @RequestBody CreateListRequest request) {
        ShoppingList list = shoppingListService.createList(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShoppingList> getList(@PathVariable Long id) {
        ShoppingList list = shoppingListService.getList(id);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<ListMember> inviteUser(@PathVariable Long id,
                                                 @Valid @RequestBody InviteRequest request) {
        ListMember member = shoppingListService.inviteUser(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }
}
