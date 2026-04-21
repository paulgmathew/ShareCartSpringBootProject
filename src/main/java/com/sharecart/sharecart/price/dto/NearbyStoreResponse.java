package com.sharecart.sharecart.price.dto;

public record NearbyStoreResponse(
        StoreResponse store,
        double distanceMeters
) {
}
