package com.sharecart.sharecart.price.service.impl;

import com.sharecart.sharecart.price.dto.NearbyStoreResponse;
import com.sharecart.sharecart.price.dto.StoreResponse;
import com.sharecart.sharecart.price.model.Store;
import com.sharecart.sharecart.price.repository.StoreRepository;
import com.sharecart.sharecart.price.service.StoreService;
import com.sharecart.sharecart.price.util.HaversineDistanceUtil;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private static final double BOUNDING_BOX_DELTA = 0.02;
    private static final double DUPLICATE_STORE_DISTANCE_METERS = 200.0;
    private static final int NEARBY_RESULTS_LIMIT = 10;

    private final StoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NearbyStoreResponse> findNearbyStores(Double latitude, Double longitude) {
        List<Store> nearbyCandidates = storeRepository.findByBoundingBox(
                latitude - BOUNDING_BOX_DELTA,
                latitude + BOUNDING_BOX_DELTA,
                longitude - BOUNDING_BOX_DELTA,
                longitude + BOUNDING_BOX_DELTA
        );

        return nearbyCandidates.stream()
                .map(store -> new NearbyStoreResponse(
                        toResponse(store),
                        HaversineDistanceUtil.distanceInMeters(
                                latitude,
                                longitude,
                                store.getLatitude(),
                                store.getLongitude()
                        )
                ))
                .sorted(Comparator.comparingDouble(NearbyStoreResponse::distanceMeters))
                .limit(NEARBY_RESULTS_LIMIT)
                .toList();
    }

    @Override
    @Transactional
    public StoreResponse createStoreIfNotExists(String name, String address, Double latitude, Double longitude) {
        Store store = resolveStore(name, address, latitude, longitude);
        return toResponse(store);
    }

    @Override
    @Transactional
    public Store resolveStore(String name, String address, Double latitude, Double longitude) {
        List<Store> sameNameStores = storeRepository.findAllByNameIgnoreCase(name.trim());

        for (Store existing : sameNameStores) {
            if (existing.getLatitude() == null || existing.getLongitude() == null) {
                continue;
            }

            double distance = HaversineDistanceUtil.distanceInMeters(
                    latitude,
                    longitude,
                    existing.getLatitude(),
                    existing.getLongitude()
            );

            if (distance < DUPLICATE_STORE_DISTANCE_METERS) {
                return existing;
            }
        }

        Store created = Store.builder()
                .name(name.trim())
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .build();

        return storeRepository.save(created);
    }

    private StoreResponse toResponse(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getLatitude(),
                store.getLongitude(),
                store.getCreatedAt()
        );
    }
}
