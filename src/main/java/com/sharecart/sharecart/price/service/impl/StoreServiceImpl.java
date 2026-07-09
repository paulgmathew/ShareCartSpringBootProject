package com.sharecart.sharecart.price.service.impl;

import com.sharecart.sharecart.price.dto.NearbyStoreResponse;
import com.sharecart.sharecart.price.dto.StoreInfoRequest;
import com.sharecart.sharecart.price.dto.StoreResponse;
import com.sharecart.sharecart.price.model.Store;
import com.sharecart.sharecart.price.repository.StoreRepository;
import com.sharecart.sharecart.price.service.StoreResolverService;
import com.sharecart.sharecart.price.service.StoreService;
import com.sharecart.sharecart.price.util.HaversineDistanceUtil;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private static final double BOUNDING_BOX_DELTA = 0.02;
    private static final int NEARBY_RESULTS_LIMIT = 10;

    private final StoreRepository storeRepository;
    private final StoreResolverService storeResolverService;

    @Override
    @Transactional(readOnly = true)
    public List<NearbyStoreResponse> findNearbyStores(Double latitude, Double longitude) {
        log.debug("Searching nearby stores lat={} lon={} delta={}", latitude, longitude, BOUNDING_BOX_DELTA);
        List<Store> nearbyCandidates = storeRepository.findByBoundingBox(
                latitude - BOUNDING_BOX_DELTA,
                latitude + BOUNDING_BOX_DELTA,
                longitude - BOUNDING_BOX_DELTA,
                longitude + BOUNDING_BOX_DELTA
        );

        List<NearbyStoreResponse> results = nearbyCandidates.stream()
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

        log.info("Found {} nearby store(s) lat={} lon={}", results.size(), latitude, longitude);
        return results;
    }

    @Override
    @Transactional
    public StoreResponse createStoreIfNotExists(String name, String address, Double latitude, Double longitude) {
        log.info("Resolving store name={} lat={} lon={}", name, latitude, longitude);
        Store store = resolveStore(name, address, latitude, longitude);
        log.info("Store resolved storeId={} name={}", store.getId(), store.getName());
        return toResponse(store);
    }

    @Override
    @Transactional
    public Store resolveStore(String name, String address, Double latitude, Double longitude) {
        return storeResolverService.resolve(new StoreInfoRequest(name, address, latitude, longitude));
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
