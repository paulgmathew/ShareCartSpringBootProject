package com.sharecart.sharecart.price.service;

import com.sharecart.sharecart.price.dto.ComparePriceRequest;
import com.sharecart.sharecart.price.dto.ComparePriceResponse;
import com.sharecart.sharecart.price.dto.ConfirmPriceRequest;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureRequest;
import com.sharecart.sharecart.price.dto.CreatePriceCaptureResponse;
import com.sharecart.sharecart.price.dto.ItemPriceResponse;
import java.util.UUID;

public interface PriceService {

    String normalizeItemName(String itemName);

    CreatePriceCaptureResponse createCapture(CreatePriceCaptureRequest request, UUID userId);

    ItemPriceResponse confirmPrice(ConfirmPriceRequest request, UUID userId);

    ComparePriceResponse comparePrice(ComparePriceRequest request);
}
