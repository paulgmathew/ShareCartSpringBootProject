package com.sharecart.sharecart.price.repository;

import com.sharecart.sharecart.price.model.ItemPrice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPriceRepository extends JpaRepository<ItemPrice, UUID> {

    List<ItemPrice> findByNormalizedName(String normalizedName);

    Optional<ItemPrice> findTopByNormalizedNameAndStoreIdOrderByCreatedAtDesc(String normalizedName, UUID storeId);
}
