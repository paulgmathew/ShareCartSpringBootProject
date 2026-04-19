package com.sharecart.sharecart.shoppinglist.service;

import java.util.UUID;

public interface ListAccessService {

    boolean canAccessList(UUID userId, UUID listId);
}