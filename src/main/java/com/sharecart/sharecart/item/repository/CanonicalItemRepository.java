package com.sharecart.sharecart.item.repository;

import com.sharecart.sharecart.item.model.CanonicalItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanonicalItemRepository extends JpaRepository<CanonicalItem, UUID> {

    Optional<CanonicalItem> findByNormalizedName(String normalizedName);

    List<CanonicalItem> findByNormalizedNameContaining(String normalizedName);
}
