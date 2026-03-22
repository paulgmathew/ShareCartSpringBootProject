package com.sharecart.service;

import com.sharecart.dto.CreateListRequest;
import com.sharecart.dto.InviteRequest;
import com.sharecart.entity.ListMember;
import com.sharecart.entity.ShoppingList;
import com.sharecart.entity.User;
import com.sharecart.exception.AlreadyMemberException;
import com.sharecart.exception.ResourceNotFoundException;
import com.sharecart.repository.ListMemberRepository;
import com.sharecart.repository.ShoppingListRepository;
import com.sharecart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingListService {

    private final ShoppingListRepository shoppingListRepository;
    private final UserRepository userRepository;
    private final ListMemberRepository listMemberRepository;

    public ShoppingListService(ShoppingListRepository shoppingListRepository,
                               UserRepository userRepository,
                               ListMemberRepository listMemberRepository) {
        this.shoppingListRepository = shoppingListRepository;
        this.userRepository = userRepository;
        this.listMemberRepository = listMemberRepository;
    }

    @Transactional
    public ShoppingList createList(CreateListRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getOwnerId()));
        ShoppingList list = new ShoppingList(request.getName(), owner);
        return shoppingListRepository.save(list);
    }

    @Transactional(readOnly = true)
    public ShoppingList getList(Long id) {
        return shoppingListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shopping list not found with id: " + id));
    }

    @Transactional
    public ListMember inviteUser(Long listId, InviteRequest request) {
        ShoppingList list = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shopping list not found with id: " + listId));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getUserId()));

        if (listMemberRepository.existsByListAndUser(list, user)) {
            throw new AlreadyMemberException(
                    "User " + user.getId() + " is already a member of list " + listId);
        }

        ListMember member = new ListMember(list, user);
        return listMemberRepository.save(member);
    }
}
