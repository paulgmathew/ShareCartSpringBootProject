package com.sharecart.sharecart.price.service;

import com.sharecart.sharecart.price.dto.ComparePriceRequest;
import com.sharecart.sharecart.price.dto.ComparePriceResponse;
import com.sharecart.sharecart.price.dto.BestPriceSummaryResponse;
import com.sharecart.sharecart.price.dto.ConfirmPriceRequest;
import com.sharecart.sharecart.price.dto.ConfirmPriceResponse;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureRequest;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureResponse;
import com.sharecart.sharecart.price.dto.ItemPriceResponse;
import com.sharecart.sharecart.price.dto.StorePriceResponse;
import java.util.List;
import java.util.UUID;

public interface PriceService {

    String normalizeItemName(String itemName);

    CreatePriceCaptureResponse createCapture(CreatePriceCaptureRequest request, UUID userId);

    ConfirmPriceResponse confirmPrice(ConfirmPriceRequest request, UUID userId);

    ComparePriceResponse comparePrice(ComparePriceRequest request, UUID userId);

    List<ItemPriceResponse> getPriceHistory(UUID userId, String itemNameFilter);

    void deletePriceHistoryEntry(UUID userId, UUID priceId);

    List<StorePriceResponse> getLowestPriceByStore(UUID userId, UUID canonicalItemId);

    List<BestPriceSummaryResponse> getBestPriceSummary(UUID userId);
}
