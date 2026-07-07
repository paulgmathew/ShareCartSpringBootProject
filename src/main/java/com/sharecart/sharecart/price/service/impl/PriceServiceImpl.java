package com.sharecart.sharecart.price.service.impl;

import com.sharecart.sharecart.common.exception.ResourceNotFoundException;
import com.sharecart.sharecart.price.dto.ComparePriceRequest;
import com.sharecart.sharecart.price.dto.ComparePriceResponse;
import com.sharecart.sharecart.price.dto.ConfirmPriceItemRequest;
import com.sharecart.sharecart.price.dto.ConfirmPriceRequest;
import com.sharecart.sharecart.price.dto.ConfirmPriceResponse;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureRequest;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureResponse;
import com.sharecart.sharecart.price.model.ItemPrice;
import com.sharecart.sharecart.price.model.PriceCapture;
import com.sharecart.sharecart.price.model.Store;
import com.sharecart.sharecart.price.repository.ItemPriceRepository;
import com.sharecart.sharecart.price.repository.PriceCaptureRepository;
import com.sharecart.sharecart.price.service.PriceService;
import com.sharecart.sharecart.price.service.StoreResolverService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriceServiceImpl implements PriceService {

    private final PriceCaptureRepository priceCaptureRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final StoreResolverService storeResolverService;

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
    public ConfirmPriceResponse confirmPrice(ConfirmPriceRequest request, UUID userId) {
        priceCaptureRepository.findById(request.captureId())
                .orElseThrow(() -> new ResourceNotFoundException("Capture not found with id: " + request.captureId()));

        Store store = storeResolverService.resolve(request.store());
        String source = normalizeSource(request.source());
        LocalDateTime capturedAt = request.capturedAt().toLocalDateTime();

        List<UUID> savedIds = new ArrayList<>();
        for (ConfirmPriceItemRequest item : request.items()) {
            ItemPrice saved = saveConfirmedPrice(item, store, source, capturedAt, userId);
            savedIds.add(saved.getId());
        }

        if (savedIds.size() == 1) {
            UUID savedId = savedIds.get(0);
            return new ConfirmPriceResponse(savedId, 1, List.of(savedId), "Price saved successfully");
        }

        return new ConfirmPriceResponse(null, savedIds.size(), List.copyOf(savedIds), "Confirmed prices saved");
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

    private ItemPrice saveConfirmedPrice(
            ConfirmPriceItemRequest item,
            Store store,
            String source,
            LocalDateTime capturedAt,
            UUID userId
    ) {
        String normalizedName = normalizeItemName(item.itemName());
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Item name is required");
        }

        return itemPriceRepository.save(ItemPrice.builder()
                .itemName(item.itemName().trim())
                .normalizedName(normalizedName)
                .store(store)
                .price(item.price())
                .unit(item.unit())
                .source(source)
                .createdBy(userId)
                .capturedAt(capturedAt)
                .build());
    }

    private String normalizeSource(String source) {
        String normalizedSource = source.trim().toUpperCase(Locale.ROOT);
        if (!List.of("MANUAL", "OCR", "API").contains(normalizedSource)) {
            throw new IllegalArgumentException("Source must be MANUAL, OCR, or API");
        }

        return normalizedSource;
    }
}
