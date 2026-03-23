package com.sharecart.sharecart.shoppinglist.service;

import com.sharecart.sharecart.shoppinglist.dto.CreateListRequest;
import com.sharecart.sharecart.shoppinglist.dto.InviteRequest;
import com.sharecart.sharecart.shoppinglist.dto.MyListResponse;
import com.sharecart.sharecart.shoppinglist.dto.ShoppingListResponse;
import java.util.List;
import java.util.UUID;

public interface ShoppingListService {

    ShoppingListResponse createList(CreateListRequest request, UUID ownerId);

    List<MyListResponse> getMyLists(UUID userId);

    ShoppingListResponse getListById(UUID id);

    void inviteUser(UUID listId, InviteRequest request);
}
