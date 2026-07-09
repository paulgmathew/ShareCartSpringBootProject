package com.sharecart.sharecart.price.controller;

import com.sharecart.sharecart.price.dto.ComparePriceRequest;
import com.sharecart.sharecart.price.dto.ComparePriceResponse;
import com.sharecart.sharecart.price.dto.ConfirmPriceRequest;
import com.sharecart.sharecart.price.dto.ConfirmPriceResponse;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureRequest;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureResponse;
import com.sharecart.sharecart.price.dto.ItemPriceResponse;
import com.sharecart.sharecart.price.service.PriceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @PostMapping("/capture")
    public ResponseEntity<CreatePriceCaptureResponse> createCapture(@Valid @RequestBody CreatePriceCaptureRequest request) {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        CreatePriceCaptureResponse created = priceService.createCapture(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmPriceResponse> confirmPrice(@Valid @RequestBody ConfirmPriceRequest request) {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED).body(priceService.confirmPrice(request, userId));
    }

    @PostMapping("/compare")
    public ResponseEntity<ComparePriceResponse> comparePrice(@Valid @RequestBody ComparePriceRequest request) {
        return ResponseEntity.ok(priceService.comparePrice(request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ItemPriceResponse>> getPriceHistory(
            @RequestParam(required = false) String itemName) {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return ResponseEntity.ok(priceService.getPriceHistory(userId, itemName));
    }
}
