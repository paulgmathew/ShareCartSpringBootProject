package com.sharecart.sharecart.price.controller;

import com.sharecart.sharecart.price.dto.CreateStoreRequest;
import com.sharecart.sharecart.price.dto.NearbyStoreResponse;
import com.sharecart.sharecart.price.dto.StoreResponse;
import com.sharecart.sharecart.price.service.StoreService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@Validated
public class StoreController {

    private final StoreService storeService;

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyStoreResponse>> findNearbyStores(
            @RequestParam @NotNull(message = "Latitude is required") Double lat,
            @RequestParam @NotNull(message = "Longitude is required") Double lon) {
        return ResponseEntity.ok(storeService.findNearbyStores(lat, lon));
    }

    @PostMapping
    public ResponseEntity<StoreResponse> createStore(@Valid @RequestBody CreateStoreRequest request) {
        StoreResponse created = storeService.createStoreIfNotExists(
                request.name(),
                request.address(),
                request.latitude(),
                request.longitude()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
