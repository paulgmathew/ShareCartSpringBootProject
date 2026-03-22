package com.sharecart.sharecart.shoppinglist.service;

import com.sharecart.sharecart.shoppinglist.dto.CreateListRequest;
import com.sharecart.sharecart.shoppinglist.dto.InviteRequest;
import com.sharecart.sharecart.shoppinglist.dto.ShoppingListResponse;
import java.util.UUID;

public interface ShoppingListService {

    ShoppingListResponse createList(CreateListRequest request);

    ShoppingListResponse getListById(UUID id);

    void inviteUser(UUID listId, InviteRequest request);
}
