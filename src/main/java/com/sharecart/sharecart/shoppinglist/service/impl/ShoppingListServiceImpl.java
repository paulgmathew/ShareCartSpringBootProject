package com.sharecart.sharecart.shoppinglist.service.impl;

import com.sharecart.sharecart.common.exception.ResourceNotFoundException;
import com.sharecart.sharecart.item.dto.ItemResponse;
import com.sharecart.sharecart.item.repository.ItemRepository;
import com.sharecart.sharecart.shoppinglist.dto.CreateListRequest;
import com.sharecart.sharecart.shoppinglist.dto.InviteRequest;
import com.sharecart.sharecart.shoppinglist.dto.MemberResponse;
import com.sharecart.sharecart.shoppinglist.dto.MyListResponse;
import com.sharecart.sharecart.shoppinglist.dto.ShoppingListResponse;
import com.sharecart.sharecart.shoppinglist.model.ListMember;
import com.sharecart.sharecart.shoppinglist.model.ShoppingList;
import com.sharecart.sharecart.shoppinglist.repository.ListMemberRepository;
import com.sharecart.sharecart.shoppinglist.repository.ShoppingListRepository;
import com.sharecart.sharecart.shoppinglist.service.ShoppingListService;
import com.sharecart.sharecart.user.model.User;
import com.sharecart.sharecart.user.repository.UserRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingListServiceImpl implements ShoppingListService {

    private final ShoppingListRepository shoppingListRepository;
    private final ListMemberRepository listMemberRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ShoppingListResponse createList(CreateListRequest request, UUID ownerId) {
        log.debug("Creating shopping list name={} ownerId={}", request.name(), ownerId);
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + ownerId));

        ShoppingList shoppingList = ShoppingList.builder()
                .name(request.name())
                .owner(owner)
                .build();

        ShoppingList saved = shoppingListRepository.save(shoppingList);

        ListMember ownerMember = ListMember.builder()
                .shoppingList(saved)
                .user(owner)
                .role("OWNER")
                .build();
        listMemberRepository.save(ownerMember);

        log.info("Shopping list created listId={} ownerId={}", saved.getId(), ownerId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyListResponse> getMyLists(UUID userId) {
        log.debug("Fetching lists for userId={}", userId);
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<UUID, MyListResponse> lists = new LinkedHashMap<>();

        for (ShoppingList list : shoppingListRepository.findByOwnerId(userId)) {
            lists.put(list.getId(), new MyListResponse(
                    list.getId(),
                    list.getName(),
                    list.getOwner() != null ? list.getOwner().getId() : null,
                    list.getOwner() != null ? list.getOwner().getName() : null,
                    "OWNER",
                    list.getCreatedAt(),
                    list.getUpdatedAt()
            ));
        }

        for (ListMember member : listMemberRepository.findByUserId(userId)) {
            ShoppingList list = member.getShoppingList();
            String role = member.getRole() != null ? member.getRole() : "MEMBER";

            lists.put(list.getId(), new MyListResponse(
                    list.getId(),
                    list.getName(),
                    list.getOwner() != null ? list.getOwner().getId() : currentUser.getId(),
                    list.getOwner() != null ? list.getOwner().getName() : currentUser.getName(),
                    role,
                    list.getCreatedAt(),
                    list.getUpdatedAt()
            ));
        }

        List<MyListResponse> result = lists.values().stream()
                .sorted(Comparator.comparing(MyListResponse::updatedAt).reversed())
                .toList();
        log.info("Lists fetched userId={} count={}", userId, result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ShoppingListResponse getListById(UUID id) {
        log.debug("Fetching shopping list listId={}", id);
        ShoppingList shoppingList = shoppingListRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Shopping list not found listId={}", id);
                    return new ResourceNotFoundException("Shopping list not found with id: " + id);
                });

        log.info("Shopping list fetched listId={}", id);
        return toResponse(shoppingList);
    }

    @Override
    @Transactional
    public void inviteUser(UUID listId, InviteRequest request) {
        log.debug("Inviting userId={} to listId={}", request.userId(), listId);
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping list not found with id: " + listId));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.userId()));

        listMemberRepository.findByShoppingListIdAndUserId(listId, request.userId())
                .ifPresent(m -> {
                    log.warn("User already a member listId={} userId={}", listId, request.userId());
                    throw new IllegalStateException("User is already a member of this list");
                });

        String role = (request.role() != null && !request.role().isBlank())
                ? request.role().toUpperCase()
                : "MEMBER";

        ListMember member = ListMember.builder()
                .shoppingList(shoppingList)
                .user(user)
                .role(role)
                .build();

        listMemberRepository.save(member);
        log.info("User invited listId={} userId={} role={}", listId, user.getId(), role);
    }

    private ShoppingListResponse toResponse(ShoppingList shoppingList) {
        List<ItemResponse> items = itemRepository.findByShoppingListId(shoppingList.getId()).stream()
                .map(item -> new ItemResponse(
                        item.getId(),
                        item.getShoppingList().getId(),
                        item.getName(),
                        item.getQuantity(),
                        item.getIsCompleted(),
                        item.getCategory(),
                        item.getCreatedBy() != null ? item.getCreatedBy().getId() : null,
                        item.getCreatedAt(),
                        item.getUpdatedAt()
                ))
                .toList();

        List<MemberResponse> members = listMemberRepository.findByShoppingListId(shoppingList.getId()).stream()
                .map(m -> new MemberResponse(
                        m.getUser().getId(),
                        m.getUser().getName(),
                        m.getUser().getEmail(),
                        m.getRole(),
                        m.getJoinedAt()
                ))
                .toList();

        return new ShoppingListResponse(
                shoppingList.getId(),
                shoppingList.getName(),
                shoppingList.getOwner() != null ? shoppingList.getOwner().getId() : null,
                shoppingList.getOwner() != null ? shoppingList.getOwner().getName() : null,
                items,
                members,
                shoppingList.getCreatedAt(),
                shoppingList.getUpdatedAt()
        );
    }
}
