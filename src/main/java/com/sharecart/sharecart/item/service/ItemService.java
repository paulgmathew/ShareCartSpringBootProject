package com.sharecart.sharecart.item.service;

import com.sharecart.sharecart.item.dto.CreateItemRequest;
import com.sharecart.sharecart.item.dto.ItemResponse;
import com.sharecart.sharecart.item.dto.UpdateItemRequest;
import java.util.UUID;

public interface ItemService {

    ItemResponse addItem(UUID listId, CreateItemRequest request);

    ItemResponse updateItem(UUID id, UpdateItemRequest request);

    void deleteItem(UUID id);
}
