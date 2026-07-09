package com.sharecart.sharecart.shoppinglist.controller;

import com.sharecart.sharecart.shoppinglist.dto.CreateListRequest;
import com.sharecart.sharecart.shoppinglist.dto.InviteRequest;
import com.sharecart.sharecart.shoppinglist.dto.MyListResponse;
import com.sharecart.sharecart.shoppinglist.dto.ShoppingListResponse;
import com.sharecart.sharecart.shoppinglist.service.ShoppingListService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/lists")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    // POST /api/v1/lists
    @PostMapping
    public ResponseEntity<ShoppingListResponse> createList(@Valid @RequestBody CreateListRequest request) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("POST /api/v1/lists name={} userId={}", request.name(), userId);
        ShoppingListResponse created = shoppingListService.createList(request, UUID.fromString(userId));
        log.info("Shopping list created listId={} userId={}", created.id(), userId);
        URI location = URI.create("/api/v1/lists/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    // GET /api/v1/lists/me
    @GetMapping("/me")
    public List<MyListResponse> getMyLists() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("GET /api/v1/lists/me userId={}", userId);
        List<MyListResponse> lists = shoppingListService.getMyLists(UUID.fromString(userId));
        log.info("My lists returned userId={} count={}", userId, lists.size());
        return lists;
    }

    // GET /api/v1/lists/{id}
    @GetMapping("/{id}")
    public ShoppingListResponse getListById(@PathVariable UUID id) {
        log.info("GET /api/v1/lists/{}", id);
        return shoppingListService.getListById(id);
    }

    // POST /api/v1/lists/{id}/invite
    @PostMapping("/{id}/invite")
    public ResponseEntity<Void> inviteUser(@PathVariable UUID id, @Valid @RequestBody InviteRequest request) {
        log.info("POST /api/v1/lists/{}/invite targetUserId={}", id, request.userId());
        shoppingListService.inviteUser(id, request);
        log.info("User invited listId={} targetUserId={}", id, request.userId());
        return ResponseEntity.ok().build();
    }
}
