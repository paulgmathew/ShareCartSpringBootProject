package com.sharecart.sharecart.price.controller;

import com.sharecart.sharecart.price.dto.ComparePriceRequest;
import com.sharecart.sharecart.price.dto.ComparePriceResponse;
import com.sharecart.sharecart.price.dto.BestPriceSummaryResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;
    //not used 
    @PostMapping("/capture") 
    public ResponseEntity<CreatePriceCaptureResponse> createCapture(@Valid @RequestBody CreatePriceCaptureRequest request) {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        log.info("POST /api/v1/prices/capture userId={} lat={} lon={}", userId, request.latitude(), request.longitude());
        CreatePriceCaptureResponse created = priceService.createCapture(request, userId);
        log.info("Capture created captureId={} userId={}", created.captureId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmPriceResponse> confirmPrice(@Valid @RequestBody ConfirmPriceRequest request) {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        log.info("POST /api/v1/prices/confirm captureId={} itemCount={} userId={}", request.captureId(), request.items().size(), userId);
        ConfirmPriceResponse response = priceService.confirmPrice(request, userId);
        log.info("Prices confirmed savedCount={} userId={}", response.savedCount(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    //not used 
    @PostMapping("/compare")
    public ResponseEntity<ComparePriceResponse> comparePrice(@Valid @RequestBody ComparePriceRequest request) {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        log.info("POST /api/v1/prices/compare itemName={} userId={}", request.itemName(), userId);
        ComparePriceResponse response = priceService.comparePrice(request, userId);
        log.info("Price comparison result itemName={} lowestPrice={} totalEntries={} userId={}", request.itemName(), response.lowestPrice(), response.totalEntries(), userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/best-store/{canonicalItemId}")
    public ResponseEntity<List<com.sharecart.sharecart.price.dto.StorePriceResponse>> getLowestPriceByStore(@PathVariable UUID canonicalItemId) {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        log.info("GET /api/v1/prices/best-store/{} userId={}", canonicalItemId, userId);
        return ResponseEntity.ok(priceService.getLowestPriceByStore(userId, canonicalItemId));
    }

    @GetMapping("/best-prices")
    public ResponseEntity<List<BestPriceSummaryResponse>> getBestPriceSummary() {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        log.info("GET /api/v1/prices/best-prices userId={}", userId);
        return ResponseEntity.ok(priceService.getBestPriceSummary(userId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ItemPriceResponse>> getPriceHistory(
            @RequestParam(required = false) String itemName) {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        log.info("GET /api/v1/prices/history userId={} filter={}", userId, itemName);
        List<ItemPriceResponse> history = priceService.getPriceHistory(userId, itemName);
        log.info("Price history returned userId={} count={}", userId, history.size());
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deletePriceHistoryEntry(@PathVariable UUID id) {
        UUID userId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        log.info("DELETE /api/v1/prices/history/{} userId={}", id, userId);
        priceService.deletePriceHistoryEntry(userId, id);
        return ResponseEntity.noContent().build();
    }
}
