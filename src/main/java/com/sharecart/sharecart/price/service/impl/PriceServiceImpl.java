package com.sharecart.sharecart.price.service.impl;

import com.sharecart.sharecart.common.exception.ResourceNotFoundException;
import com.sharecart.sharecart.item.model.CanonicalItem;
import com.sharecart.sharecart.item.repository.CanonicalItemRepository;
import com.sharecart.sharecart.price.dto.BestPriceSummaryResponse;
import com.sharecart.sharecart.price.dto.ComparePriceRequest;
import com.sharecart.sharecart.price.dto.ComparePriceResponse;
import com.sharecart.sharecart.price.dto.ConfirmPriceItemRequest;
import com.sharecart.sharecart.price.dto.ConfirmPriceRequest;
import com.sharecart.sharecart.price.dto.ConfirmPriceResponse;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureRequest;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureResponse;
import com.sharecart.sharecart.price.dto.ItemPriceResponse;
import com.sharecart.sharecart.price.dto.StorePriceResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceServiceImpl implements PriceService {

    private final PriceCaptureRepository priceCaptureRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final CanonicalItemRepository canonicalItemRepository;
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
        log.debug("Creating price capture for userId={} lat={} lon={}", userId, request.latitude(), request.longitude());
        PriceCapture capture = PriceCapture.builder()
                .rawText(request.rawText())
                .imageUrl(request.imageUrl())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .userId(userId)
                .build();

        PriceCapture saved = priceCaptureRepository.save(capture);
        log.info("Price capture saved captureId={} userId={}", saved.getId(), userId);
        return new CreatePriceCaptureResponse(saved.getId());
    }

    @Override
    @Transactional
    public ConfirmPriceResponse confirmPrice(ConfirmPriceRequest request, UUID userId) {
        log.debug("Confirming prices captureId={} itemCount={} userId={}", request.captureId(), request.items().size(), userId);
        priceCaptureRepository.findById(request.captureId())
                .orElseThrow(() -> {
                    log.warn("Capture not found captureId={}", request.captureId());
                    return new ResourceNotFoundException("Capture not found with id: " + request.captureId());
                });

        Store store = storeResolverService.resolve(request.store());
        String source = normalizeSource(request.source());
        LocalDateTime capturedAt = request.capturedAt().toLocalDateTime();

        List<UUID> savedIds = new ArrayList<>();
        for (ConfirmPriceItemRequest item : request.items()) {
            ItemPrice saved = saveConfirmedPrice(item, store, source, capturedAt, userId);
            savedIds.add(saved.getId());
        }

        log.info("Confirmed {} price(s) storeId={} source={} userId={}", savedIds.size(), store.getId(), source, userId);

        if (savedIds.size() == 1) {
            UUID savedId = savedIds.get(0);
            return new ConfirmPriceResponse(savedId, 1, List.of(savedId), "Price saved successfully");
        }

        return new ConfirmPriceResponse(null, savedIds.size(), List.copyOf(savedIds), "Confirmed prices saved");
    }

    @Override
    @Transactional(readOnly = true)
    public ComparePriceResponse comparePrice(ComparePriceRequest request, UUID userId) {
        String normalizedName = normalizeItemName(request.itemName());
        log.debug("Comparing prices normalizedName={} userId={}", normalizedName, userId);
        List<ItemPrice> entries = itemPriceRepository.findByNormalizedNameAndCreatedBy(normalizedName, userId);

        if (entries.isEmpty()) {
            log.warn("No prices found for item normalizedName={}", normalizedName);
            throw new ResourceNotFoundException("No prices found for item: " + request.itemName());
        }

        ItemPrice lowest = entries.stream()
                .min(Comparator.comparing(ItemPrice::getPrice))
                .orElseThrow();

        BigDecimal total = entries.stream()
                .map(ItemPrice::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = total.divide(BigDecimal.valueOf(entries.size()), 2, RoundingMode.HALF_UP);

        log.info("Price comparison normalizedName={} lowestPrice={} avgPrice={} entryCount={} userId={}", normalizedName, lowest.getPrice(), average, entries.size(), userId);
        return new ComparePriceResponse(
                lowest.getPrice(),
                lowest.getStore().getId(),
            lowest.getStore().getName(),
                average,
                entries.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemPriceResponse> getPriceHistory(UUID userId, String itemNameFilter) {
        String normalizedFilter = normalizeItemName(itemNameFilter);
        log.debug("Fetching price history userId={} normalizedFilter={}", userId, normalizedFilter.isBlank() ? "(none)" : normalizedFilter);

        List<ItemPrice> entries;
        if (normalizedFilter.isBlank()) {
            entries = itemPriceRepository.findByCreatedByOrderByCreatedAtDesc(userId);
        } else {
            entries = itemPriceRepository.findByCreatedByAndNormalizedNameContainingOrderByCreatedAtDesc(
                    userId,
                    normalizedFilter
            );
        }

        log.info("Price history fetched userId={} resultCount={}", userId, entries.size());
        return entries.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorePriceResponse> getLowestPriceByStore(UUID userId, UUID canonicalItemId) {
        if (!canonicalItemRepository.existsById(canonicalItemId)) {
            throw new ResourceNotFoundException("Canonical item not found with id: " + canonicalItemId);
        }
        return itemPriceRepository.findLowestPriceByStoreForUserAndCanonicalItem(userId, canonicalItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BestPriceSummaryResponse> getBestPriceSummary(UUID userId) {
        List<ItemPriceRepository.BestPriceSummaryRow> rows = itemPriceRepository.findBestPriceSummaryRowsByUserId(userId);
        Map<UUID, BestPriceSummaryResponse> bestByCanonicalItem = new LinkedHashMap<>();

        for (ItemPriceRepository.BestPriceSummaryRow row : rows) {
            if (row.getCanonicalItemId() == null) {
                continue;
            }

            BestPriceSummaryResponse candidate = new BestPriceSummaryResponse(
                    row.getCanonicalItemId(),
                    row.getItemName(),
                    row.getLowestPrice(),
                    row.getStoreId(),
                    row.getStoreName()
            );

            bestByCanonicalItem.merge(
                    row.getCanonicalItemId(),
                    candidate,
                    (current, incoming) -> isBetterBestPrice(incoming, current) ? incoming : current
            );
        }

        return bestByCanonicalItem.values().stream()
                .sorted(Comparator.comparing(BestPriceSummaryResponse::itemName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(BestPriceSummaryResponse::canonicalItemId))
                .toList();
    }

    @Override
    @Transactional
    public void deletePriceHistoryEntry(UUID userId, UUID priceId) {
        ItemPrice itemPrice = itemPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("Price history entry not found with id: " + priceId));

        if (!userId.equals(itemPrice.getCreatedBy())) {
            log.warn("Forbidden price history delete attempt priceId={} ownerId={} requesterId={}", priceId, itemPrice.getCreatedBy(), userId);
            throw new AccessDeniedException("You cannot delete this price history entry");
        }

        itemPriceRepository.delete(itemPrice);
        log.info("Deleted price history entry priceId={} userId={}", priceId, userId);
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

        CanonicalItem canonicalItem = null;
        if (item.canonicalItemId() != null) {
            canonicalItem = canonicalItemRepository.findById(item.canonicalItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Canonical item not found with id: " + item.canonicalItemId()));
        }

        return itemPriceRepository.save(ItemPrice.builder()
                .itemName(item.itemName().trim())
                .normalizedName(normalizedName)
                .store(store)
                .price(item.price())
                .unit(item.unit())
                .source(source)
                .createdBy(userId)
                .canonicalItem(canonicalItem)
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

    private boolean isBetterBestPrice(BestPriceSummaryResponse candidate, BestPriceSummaryResponse current) {
        int priceComparison = candidate.lowestPrice().compareTo(current.lowestPrice());
        if (priceComparison != 0) {
            return priceComparison < 0;
        }

        int storeNameComparison = candidate.storeName().compareToIgnoreCase(current.storeName());
        if (storeNameComparison != 0) {
            return storeNameComparison < 0;
        }

        return candidate.storeId().compareTo(current.storeId()) < 0;
    }
}
