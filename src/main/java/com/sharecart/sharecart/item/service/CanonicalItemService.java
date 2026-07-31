package com.sharecart.sharecart.item.service;

import com.sharecart.sharecart.item.dto.CanonicalItemResponse;
import com.sharecart.sharecart.item.dto.CreateCanonicalItemRequest;
import java.util.List;
import java.util.UUID;

public interface CanonicalItemService {
    CanonicalItemResponse createCanonicalItem(CreateCanonicalItemRequest request);
    List<CanonicalItemResponse> listCanonicalItems();
    CanonicalItemResponse getCanonicalItem(UUID id);
}
