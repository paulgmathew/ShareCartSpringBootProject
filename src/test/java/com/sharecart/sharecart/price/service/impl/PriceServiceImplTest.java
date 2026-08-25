package com.sharecart.sharecart.price.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sharecart.sharecart.common.exception.ResourceNotFoundException;
import com.sharecart.sharecart.item.model.CanonicalItem;
import com.sharecart.sharecart.item.repository.CanonicalItemRepository;
import com.sharecart.sharecart.price.dto.BestPriceSummaryResponse;
import com.sharecart.sharecart.price.dto.ComparePriceRequest;
import com.sharecart.sharecart.price.dto.ConfirmPriceItemRequest;
import com.sharecart.sharecart.price.dto.ConfirmPriceRequest;
import com.sharecart.sharecart.price.dto.StoreInfoRequest;
import com.sharecart.sharecart.price.dto.StorePriceResponse;
import com.sharecart.sharecart.price.model.ItemPrice;
import com.sharecart.sharecart.price.model.PriceCapture;
import com.sharecart.sharecart.price.model.Store;
import com.sharecart.sharecart.price.repository.ItemPriceRepository;
import com.sharecart.sharecart.price.repository.PriceCaptureRepository;
import com.sharecart.sharecart.price.service.StoreResolverService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PriceServiceImplTest {

    @Mock
    private PriceCaptureRepository priceCaptureRepository;

    @Mock
    private ItemPriceRepository itemPriceRepository;

    @Mock
    private CanonicalItemRepository canonicalItemRepository;

    @Mock
    private StoreResolverService storeResolverService;

    private PriceServiceImpl priceService;

    @BeforeEach
    void setUp() {
        priceService = new PriceServiceImpl(priceCaptureRepository, itemPriceRepository, canonicalItemRepository, storeResolverService);
    }

    @Test
    void shouldSaveSingleConfirmedPrice() {
        UUID captureId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Store store = Store.builder().id(UUID.randomUUID()).name("Walmart").build();
        StoreInfoRequest storeInfo = new StoreInfoRequest("Walmart", "Dallas", 32.99, -96.70);

        when(priceCaptureRepository.findById(captureId)).thenReturn(Optional.of(PriceCapture.builder().id(captureId).build()));
        when(storeResolverService.resolve(storeInfo)).thenReturn(store);
        when(itemPriceRepository.save(any(ItemPrice.class))).thenAnswer(invocation -> {
            ItemPrice itemPrice = invocation.getArgument(0);
            itemPrice.setId(UUID.randomUUID());
            return itemPrice;
        });

        var response = priceService.confirmPrice(
                new ConfirmPriceRequest(
                        captureId,
                        "PRICE_TAG",
                        "API",
                        OffsetDateTime.parse("2026-04-21T10:05:00Z"),
                        storeInfo,
                        List.of(new ConfirmPriceItemRequest("Milk (1L)", new BigDecimal("3.49"), "1L", null))
                ),
                userId
        );

        assertNotNull(response.id());
        assertEquals(1, response.savedCount());
        assertEquals(1, response.ids().size());
        assertEquals("Price saved successfully", response.message());
    }

    @Test
    void shouldSaveBulkConfirmedPrices() {
        UUID captureId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Store store = Store.builder().id(UUID.randomUUID()).name("Walmart").build();
        StoreInfoRequest storeInfo = new StoreInfoRequest("Walmart", "Dallas", 32.99, -96.70);

        when(priceCaptureRepository.findById(captureId)).thenReturn(Optional.of(PriceCapture.builder().id(captureId).build()));
        when(storeResolverService.resolve(storeInfo)).thenReturn(store);
        when(itemPriceRepository.save(any(ItemPrice.class))).thenAnswer(invocation -> {
            ItemPrice itemPrice = invocation.getArgument(0);
            itemPrice.setId(UUID.randomUUID());
            return itemPrice;
        });

        var response = priceService.confirmPrice(
                new ConfirmPriceRequest(
                        captureId,
                        "RECEIPT",
                        "API",
                        OffsetDateTime.parse("2026-04-21T10:05:00Z"),
                        storeInfo,
                        List.of(
                                new ConfirmPriceItemRequest(
                                        "Milk (1L)",
                                        new BigDecimal("3.49"),
                                        "1L",
                                        null
                                ),
                                new ConfirmPriceItemRequest(
                                        "Eggs",
                                        new BigDecimal("4.29"),
                                        "12 pack",
                                        null
                                )
                        )
                ),
                userId
        );

        assertEquals(2, response.savedCount());
        assertEquals(2, response.ids().size());
        assertEquals("Confirmed prices saved", response.message());
    }

    @Test
    void shouldReturnPriceHistoryWithoutFilter() {
        UUID userId = UUID.randomUUID();
        Store store = Store.builder().id(UUID.randomUUID()).name("Walmart").build();

        ItemPrice first = ItemPrice.builder()
            .id(UUID.randomUUID())
            .itemName("Milk")
            .normalizedName("milk")
            .store(store)
            .price(new BigDecimal("3.49"))
            .unit("1L")
            .source("API")
            .createdBy(userId)
            .build();
        first.setCreatedAt(java.time.LocalDateTime.now());
        first.setCapturedAt(java.time.LocalDateTime.now());

        ItemPrice second = ItemPrice.builder()
            .id(UUID.randomUUID())
            .itemName("Eggs")
            .normalizedName("eggs")
            .store(store)
            .price(new BigDecimal("4.29"))
            .unit("12 pack")
            .source("API")
            .createdBy(userId)
            .build();
        second.setCreatedAt(java.time.LocalDateTime.now().minusHours(1));
        second.setCapturedAt(java.time.LocalDateTime.now().minusHours(1));

        when(itemPriceRepository.findByCreatedByOrderByCreatedAtDesc(userId)).thenReturn(List.of(first, second));

        var response = priceService.getPriceHistory(userId, null);

        assertEquals(2, response.size());
        assertEquals(first.getId(), response.get(0).id());
        assertEquals(store.getId(), response.get(0).storeId());
        assertEquals(store.getName(), response.get(0).storeName());
        verify(itemPriceRepository).findByCreatedByOrderByCreatedAtDesc(userId);
        verify(itemPriceRepository, never())
            .findByCreatedByAndNormalizedNameContainingOrderByCreatedAtDesc(any(UUID.class), any(String.class));
    }

    @Test
    void shouldReturnPriceHistoryWithFilter() {
        UUID userId = UUID.randomUUID();
        Store store = Store.builder().id(UUID.randomUUID()).name("Walmart").build();

        ItemPrice milk = ItemPrice.builder()
            .id(UUID.randomUUID())
            .itemName("Whole Milk")
            .normalizedName("whole milk")
            .store(store)
            .price(new BigDecimal("5.10"))
            .unit("2L")
            .source("OCR")
            .createdBy(userId)
            .build();
        milk.setCreatedAt(java.time.LocalDateTime.now());
        milk.setCapturedAt(java.time.LocalDateTime.now());

        when(itemPriceRepository.findByCreatedByAndNormalizedNameContainingOrderByCreatedAtDesc(userId, "milk"))
            .thenReturn(List.of(milk));

        var response = priceService.getPriceHistory(userId, "  Milk  ");

        assertEquals(1, response.size());
        assertEquals(milk.getId(), response.get(0).id());
        assertEquals("whole milk", response.get(0).normalizedName());
        verify(itemPriceRepository).findByCreatedByAndNormalizedNameContainingOrderByCreatedAtDesc(userId, "milk");
        verify(itemPriceRepository, never()).findByCreatedByOrderByCreatedAtDesc(userId);
    }

    @Test
    void shouldAttachCanonicalItemWhenConfirmingPrice() {
        UUID captureId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID canonicalItemId = UUID.randomUUID();
        Store store = Store.builder().id(UUID.randomUUID()).name("Walmart").build();
        StoreInfoRequest storeInfo = new StoreInfoRequest("Walmart", "Dallas", 32.99, -96.70);
        CanonicalItem canonicalItem = CanonicalItem.builder().id(canonicalItemId).name("Milk").normalizedName("milk").build();

        when(priceCaptureRepository.findById(captureId)).thenReturn(Optional.of(PriceCapture.builder().id(captureId).build()));
        when(storeResolverService.resolve(storeInfo)).thenReturn(store);
        when(canonicalItemRepository.findById(canonicalItemId)).thenReturn(Optional.of(canonicalItem));
        when(itemPriceRepository.save(any(ItemPrice.class))).thenAnswer(invocation -> {
            ItemPrice itemPrice = invocation.getArgument(0);
            itemPrice.setId(UUID.randomUUID());
            return itemPrice;
        });

        priceService.confirmPrice(
                new ConfirmPriceRequest(
                        captureId,
                        "PRICE_TAG",
                        "API",
                        OffsetDateTime.parse("2026-04-21T10:05:00Z"),
                        storeInfo,
                        List.of(new ConfirmPriceItemRequest("Milk", new BigDecimal("3.49"), "1L", canonicalItemId))
                ),
                userId
        );

        verify(canonicalItemRepository).findById(canonicalItemId);
    }

    @Test
    void shouldComparePricesForCurrentUserOnly() {
        UUID userId = UUID.randomUUID();
        Store store = Store.builder().id(UUID.randomUUID()).name("Walmart").build();
        ItemPrice milk = ItemPrice.builder()
                .id(UUID.randomUUID())
                .itemName("Milk")
                .normalizedName("milk")
                .store(store)
                .price(new BigDecimal("3.49"))
                .createdBy(userId)
                .build();

        when(itemPriceRepository.findByNormalizedNameAndCreatedBy("milk", userId)).thenReturn(List.of(milk));

        var response = priceService.comparePrice(new ComparePriceRequest("Milk"), userId);

        assertEquals(new BigDecimal("3.49"), response.lowestPrice());
        assertEquals("Walmart", response.lowestStoreName());
        verify(itemPriceRepository).findByNormalizedNameAndCreatedBy("milk", userId);
    }

    @Test
    void shouldReturnLowestPriceByStore() {
        UUID userId = UUID.randomUUID();
        UUID canonicalItemId = UUID.randomUUID();
        List<StorePriceResponse> rows = List.of(
                new StorePriceResponse(UUID.randomUUID(), "Store A", new BigDecimal("3.10")),
                new StorePriceResponse(UUID.randomUUID(), "Store B", new BigDecimal("3.50"))
        );

        when(canonicalItemRepository.existsById(canonicalItemId)).thenReturn(true);
        when(itemPriceRepository.findLowestPriceByStoreForUserAndCanonicalItem(userId, canonicalItemId)).thenReturn(rows);

        var response = priceService.getLowestPriceByStore(userId, canonicalItemId);

        assertEquals(2, response.size());
        assertEquals("Store A", response.get(0).storeName());
        verify(itemPriceRepository).findLowestPriceByStoreForUserAndCanonicalItem(userId, canonicalItemId);
    }

    @Test
    void shouldReturnBestPriceSummaryForCanonicalItemsOnly() {
        UUID userId = UUID.randomUUID();
        UUID canonicalItemId = UUID.randomUUID();
        UUID storeAId = UUID.randomUUID();
        UUID storeBId = UUID.randomUUID();

        ItemPriceRepository.BestPriceSummaryRow storeBRow = bestPriceSummaryRow(
                canonicalItemId,
                "Milk",
                storeBId,
                "Store B",
                new BigDecimal("3.50")
        );
        ItemPriceRepository.BestPriceSummaryRow storeARow = bestPriceSummaryRow(
                canonicalItemId,
                "Milk",
                storeAId,
                "Store A",
                new BigDecimal("3.10")
        );
        ItemPriceRepository.BestPriceSummaryRow ignoredNullCanonicalRow = bestPriceSummaryRow(
                null,
                null,
                UUID.randomUUID(),
                "Ignored Store",
                new BigDecimal("1.00")
        );

        when(itemPriceRepository.findBestPriceSummaryRowsByUserId(userId))
                .thenReturn(List.of(storeBRow, storeARow, ignoredNullCanonicalRow));

        List<BestPriceSummaryResponse> response = priceService.getBestPriceSummary(userId);

        assertEquals(1, response.size());
        assertEquals(canonicalItemId, response.get(0).canonicalItemId());
        assertEquals("Milk", response.get(0).itemName());
        assertEquals(new BigDecimal("3.10"), response.get(0).lowestPrice());
        assertEquals(storeAId, response.get(0).storeId());
        assertEquals("Store A", response.get(0).storeName());
        verify(itemPriceRepository).findBestPriceSummaryRowsByUserId(userId);
    }

    private ItemPriceRepository.BestPriceSummaryRow bestPriceSummaryRow(
            UUID canonicalItemId,
            String itemName,
            UUID storeId,
            String storeName,
            BigDecimal lowestPrice
    ) {
        return new ItemPriceRepository.BestPriceSummaryRow() {
            @Override
            public UUID getCanonicalItemId() {
                return canonicalItemId;
            }

            @Override
            public String getItemName() {
                return itemName;
            }

            @Override
            public UUID getStoreId() {
                return storeId;
            }

            @Override
            public String getStoreName() {
                return storeName;
            }

            @Override
            public BigDecimal getLowestPrice() {
                return lowestPrice;
            }
        };
    }

    @Test
    void shouldDeleteOwnedPriceHistoryEntry() {
        UUID userId = UUID.randomUUID();
        UUID priceId = UUID.randomUUID();
        ItemPrice itemPrice = ItemPrice.builder()
                .id(priceId)
                .createdBy(userId)
                .build();

        when(itemPriceRepository.findById(priceId)).thenReturn(Optional.of(itemPrice));

        priceService.deletePriceHistoryEntry(userId, priceId);

        verify(itemPriceRepository).delete(itemPrice);
    }

    @Test
    void shouldRejectDeletingAnotherUsersPriceHistoryEntry() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID priceId = UUID.randomUUID();
        ItemPrice itemPrice = ItemPrice.builder()
                .id(priceId)
                .createdBy(otherUserId)
                .build();

        when(itemPriceRepository.findById(priceId)).thenReturn(Optional.of(itemPrice));

        assertThrows(AccessDeniedException.class, () -> priceService.deletePriceHistoryEntry(userId, priceId));

        verify(itemPriceRepository, never()).delete(any(ItemPrice.class));
    }

    @Test
    void shouldThrowWhenDeletingMissingPriceHistoryEntry() {
        UUID userId = UUID.randomUUID();
        UUID priceId = UUID.randomUUID();

        when(itemPriceRepository.findById(priceId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> priceService.deletePriceHistoryEntry(userId, priceId)
        );

        assertEquals("Price history entry not found with id: " + priceId, exception.getMessage());
        verify(itemPriceRepository, never()).delete(any(ItemPrice.class));
    }
}