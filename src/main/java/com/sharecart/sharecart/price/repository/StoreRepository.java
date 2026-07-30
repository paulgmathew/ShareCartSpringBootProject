package com.sharecart.sharecart.price.repository;

import com.sharecart.sharecart.price.model.Store;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    @Query("""
            SELECT s FROM Store s
            WHERE s.latitude BETWEEN :minLat AND :maxLat
              AND s.longitude BETWEEN :minLon AND :maxLon
            """)
    List<Store> findByBoundingBox(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon
    );

    Optional<Store> findByNameIgnoreCase(String name);

    List<Store> findAllByNameIgnoreCase(String name);
}
