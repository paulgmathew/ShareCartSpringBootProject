package com.sharecart.sharecart.price.repository;

import com.sharecart.sharecart.price.model.ItemPrice;
import com.sharecart.sharecart.price.dto.StorePriceResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ItemPriceRepository extends JpaRepository<ItemPrice, UUID> {

    interface BestPriceSummaryRow {
        UUID getCanonicalItemId();

        String getItemName();

        UUID getStoreId();

        String getStoreName();

        BigDecimal getLowestPrice();
    }

    List<ItemPrice> findByNormalizedName(String normalizedName);

    Optional<ItemPrice> findTopByNormalizedNameAndStoreIdOrderByCreatedAtDesc(String normalizedName, UUID storeId);

    List<ItemPrice> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);

    List<ItemPrice> findByCreatedByAndNormalizedNameContainingOrderByCreatedAtDesc(UUID createdBy, String normalizedName);

    Optional<ItemPrice> findByIdAndCreatedBy(UUID id, UUID createdBy);

    List<ItemPrice> findByNormalizedNameAndCreatedBy(String normalizedName, UUID createdBy);

    @Query("select new com.sharecart.sharecart.price.dto.StorePriceResponse(ip.store.id, ip.store.name, min(ip.price)) from ItemPrice ip where ip.createdBy = :userId and ip.canonicalItem.id = :canonicalItemId group by ip.store.id, ip.store.name order by min(ip.price)")
    List<StorePriceResponse> findLowestPriceByStoreForUserAndCanonicalItem(UUID userId, UUID canonicalItemId);

        @Query("""
                        select ip.canonicalItem.id as canonicalItemId,
                                     ip.canonicalItem.name as itemName,
                                     ip.store.id as storeId,
                                     ip.store.name as storeName,
                                     min(ip.price) as lowestPrice
                        from ItemPrice ip
                        where ip.createdBy = :userId
                            and ip.canonicalItem is not null
                        group by ip.canonicalItem.id, ip.canonicalItem.name, ip.store.id, ip.store.name
                        """)
        List<BestPriceSummaryRow> findBestPriceSummaryRowsByUserId(UUID userId);
}
