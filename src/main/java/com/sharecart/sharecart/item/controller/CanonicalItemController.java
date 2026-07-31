package com.sharecart.sharecart.item.controller;

import com.sharecart.sharecart.item.dto.CanonicalItemResponse;
import com.sharecart.sharecart.item.dto.CreateCanonicalItemRequest;
import com.sharecart.sharecart.item.service.CanonicalItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/items")
@RequiredArgsConstructor
public class CanonicalItemController {

    private final CanonicalItemService canonicalItemService;

    @PostMapping
    public ResponseEntity<CanonicalItemResponse> createCanonicalItem(@Valid @RequestBody CreateCanonicalItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(canonicalItemService.createCanonicalItem(request));
    }

    @GetMapping
    public ResponseEntity<List<CanonicalItemResponse>> listCanonicalItems() {
        return ResponseEntity.ok(canonicalItemService.listCanonicalItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CanonicalItemResponse> getCanonicalItem(@PathVariable UUID id) {
        return ResponseEntity.ok(canonicalItemService.getCanonicalItem(id));
    }
}
