package com.sharecart.sharecart.price.repository;

import com.sharecart.sharecart.price.model.PriceCapture;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceCaptureRepository extends JpaRepository<PriceCapture, UUID> {
}
