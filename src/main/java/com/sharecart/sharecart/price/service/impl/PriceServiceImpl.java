package com.sharecart.sharecart.price.service.impl;

import com.sharecart.sharecart.common.exception.ResourceNotFoundException;
import com.sharecart.sharecart.price.dto.ComparePriceRequest;
import com.sharecart.sharecart.price.dto.ComparePriceResponse;
import com.sharecart.sharecart.price.dto.ConfirmPriceRequest;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureRequest;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureResponse;
import com.sharecart.sharecart.price.dto.ItemPriceResponse;
import com.sharecart.sharecart.price.model.ItemPrice;
import com.sharecart.sharecart.price.model.PriceCapture;
import com.sharecart.sharecart.price.model.Store;
import com.sharecart.sharecart.price.repository.ItemPriceRepository;
import com.sharecart.sharecart.price.repository.PriceCaptureRepository;
import com.sharecart.sharecart.price.service.PriceService;
import com.sharecart.sharecart.price.service.StoreService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriceServiceImpl implements PriceService {

    private final PriceCaptureRepository priceCaptureRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final StoreService storeService;

    @Override
    public String normalizeItemName(String itemName) {
        if (itemName == null) {
            return "";
        }

        return itemName.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Override
    @Transactional
    public CreatePriceCaptureResponse createCapture(CreatePriceCaptureRequest request, UUID userId) {
        PriceCapture capture = PriceCapture.builder()
                .rawText(request.rawText())
                .imageUrl(request.imageUrl())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .userId(userId)
                .build();

        PriceCapture saved = priceCaptureRepository.save(capture);
        return new CreatePriceCaptureResponse(saved.getId());
    }

    @Override
    @Transactional
    public ItemPriceResponse confirmPrice(ConfirmPriceRequest request, UUID userId) {
        priceCaptureRepository.findById(request.captureId())
                .orElseThrow(() -> new ResourceNotFoundException("Capture not found with id: " + request.captureId()));

        String normalizedName = normalizeItemName(request.itemName());
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Item name is required");
        }

        Store store = storeService.resolveStore(
                request.storeName(),
                null,
                request.latitude(),
                request.longitude()
        );

        LocalDateTime since = LocalDateTime.now().minusHours(24);

        ItemPrice existing = itemPriceRepository
                .findTopByNormalizedNameAndStoreIdOrderByCreatedAtDesc(normalizedName, store.getId())
                .orElse(null);

        if (existing != null
                && existing.getCreatedAt() != null
                && existing.getCreatedAt().isAfter(since)
                && existing.getPrice().compareTo(request.price()) == 0) {
            return toResponse(existing);
        }

        ItemPrice saved = itemPriceRepository.save(ItemPrice.builder()
                .itemName(request.itemName().trim())
                .normalizedName(normalizedName)
                .store(store)
                .price(request.price())
                .unit(request.unit())
                .source("OCR")
                .createdBy(userId)
                .capturedAt(LocalDateTime.now())
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ComparePriceResponse comparePrice(ComparePriceRequest request) {
        String normalizedName = normalizeItemName(request.itemName());
        List<ItemPrice> entries = itemPriceRepository.findByNormalizedName(normalizedName);

        if (entries.isEmpty()) {
            throw new ResourceNotFoundException("No prices found for item: " + request.itemName());
        }

        ItemPrice lowest = entries.stream()
                .min(Comparator.comparing(ItemPrice::getPrice))
                .orElseThrow();

        BigDecimal total = entries.stream()
                .map(ItemPrice::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = total.divide(BigDecimal.valueOf(entries.size()), 2, RoundingMode.HALF_UP);

        return new ComparePriceResponse(
                lowest.getPrice(),
                lowest.getStore().getId(),
                average,
                entries.size()
        );
    }

    private ItemPriceResponse toResponse(ItemPrice itemPrice) {
        return new ItemPriceResponse(
                itemPrice.getId(),
                itemPrice.getItemName(),
                itemPrice.getNormalizedName(),
                itemPrice.getStore().getId(),
                itemPrice.getStore().getName(),
                itemPrice.getPrice(),
                itemPrice.getUnit(),
                itemPrice.getCapturedAt(),
                itemPrice.getSource(),
                itemPrice.getCreatedBy(),
                itemPrice.getCreatedAt()
        );
    }
}
