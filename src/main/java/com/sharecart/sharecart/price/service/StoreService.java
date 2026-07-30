package com.sharecart.sharecart.price.service;

import com.sharecart.sharecart.price.dto.NearbyStoreResponse;
import com.sharecart.sharecart.price.dto.StoreResponse;
import com.sharecart.sharecart.price.model.Store;
import java.util.List;

public interface StoreService {

    List<NearbyStoreResponse> findNearbyStores(Double latitude, Double longitude);

    StoreResponse createStoreIfNotExists(String name, String address, Double latitude, Double longitude);

    Store resolveStore(String name, String address, Double latitude, Double longitude);
}
