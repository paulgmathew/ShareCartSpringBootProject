package com.sharecart.sharecart.price.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sharecart.sharecart.price.dto.ConfirmPriceItemRequest;
import com.sharecart.sharecart.price.dto.ConfirmPriceRequest;
import com.sharecart.sharecart.price.dto.StoreInfoRequest;
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

@ExtendWith(MockitoExtension.class)
class PriceServiceImplTest {

    @Mock
    private PriceCaptureRepository priceCaptureRepository;

    @Mock
    private ItemPriceRepository itemPriceRepository;

    @Mock
        private StoreResolverService storeResolverService;

    private PriceServiceImpl priceService;

    @BeforeEach
    void setUp() {
        priceService = new PriceServiceImpl(priceCaptureRepository, itemPriceRepository, storeResolverService);
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
                        List.of(new ConfirmPriceItemRequest("Milk (1L)", new BigDecimal("3.49"), "1L"))
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
                                        "1L"
                                ),
                                new ConfirmPriceItemRequest(
                                        "Eggs",
                                        new BigDecimal("4.29"),
                                        "12 pack"
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
}