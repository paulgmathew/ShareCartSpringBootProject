package com.sharecart.sharecart.shoppinglist.service.impl;

import com.sharecart.sharecart.shoppinglist.repository.ListMemberRepository;
import com.sharecart.sharecart.shoppinglist.repository.ShoppingListRepository;
import com.sharecart.sharecart.shoppinglist.service.ListAccessService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListAccessServiceImpl implements ListAccessService {

    private final ShoppingListRepository shoppingListRepository;
    private final ListMemberRepository listMemberRepository;

    @Override
    public boolean canAccessList(UUID userId, UUID listId) {
        return shoppingListRepository.existsByIdAndOwnerId(listId, userId)
                || listMemberRepository.existsByShoppingListIdAndUserId(listId, userId);
    }
}