package com.sharecart.sharecart.price.service.impl;

import com.sharecart.sharecart.price.dto.StoreInfoRequest;
import com.sharecart.sharecart.price.model.Store;
import com.sharecart.sharecart.price.repository.StoreRepository;
import com.sharecart.sharecart.price.service.StoreResolverService;
import com.sharecart.sharecart.price.util.HaversineDistanceUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreResolverServiceImpl implements StoreResolverService {

    private static final double DUPLICATE_STORE_DISTANCE_METERS = 200.0;

    private final StoreRepository storeRepository;

    @Override
    public Store resolve(StoreInfoRequest request) {
        String trimmedName = request.name().trim();
        log.debug("Resolving store name={} lat={} lon={}", trimmedName, request.latitude(), request.longitude());
        List<Store> sameNameStores = storeRepository.findAllByNameIgnoreCase(trimmedName);
        log.debug("Found {} candidate(s) with name={}", sameNameStores.size(), trimmedName);

        for (Store existing : sameNameStores) {
            if (existing.getLatitude() == null || existing.getLongitude() == null) {
                continue;
            }

            double distance = HaversineDistanceUtil.distanceInMeters(
                    request.latitude(),
                    request.longitude(),
                    existing.getLatitude(),
                    existing.getLongitude()
            );

            if (distance < DUPLICATE_STORE_DISTANCE_METERS) {
                log.info("Matched existing store storeId={} name={} distanceMeters={}", existing.getId(), existing.getName(), (int) distance);
                return existing;
            }
        }

        Store created = Store.builder()
                .name(trimmedName)
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();

        Store saved = storeRepository.save(created);
        log.info("New store created storeId={} name={}", saved.getId(), saved.getName());
        return saved;
    }
}