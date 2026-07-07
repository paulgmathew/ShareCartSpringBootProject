package com.sharecart.sharecart.price.service.impl;

import com.sharecart.sharecart.price.dto.StoreInfoRequest;
import com.sharecart.sharecart.price.model.Store;
import com.sharecart.sharecart.price.repository.StoreRepository;
import com.sharecart.sharecart.price.service.StoreResolverService;
import com.sharecart.sharecart.price.util.HaversineDistanceUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreResolverServiceImpl implements StoreResolverService {

    private static final double DUPLICATE_STORE_DISTANCE_METERS = 200.0;

    private final StoreRepository storeRepository;

    @Override
    public Store resolve(StoreInfoRequest request) {
        List<Store> sameNameStores = storeRepository.findAllByNameIgnoreCase(request.name().trim());

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
                return existing;
            }
        }

        Store created = Store.builder()
                .name(request.name().trim())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();

        return storeRepository.save(created);
    }
}