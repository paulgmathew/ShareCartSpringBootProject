package com.sharecart.sharecart.invite.service.impl;

import com.sharecart.sharecart.common.exception.ExpiredInviteTokenException;
import com.sharecart.sharecart.common.exception.ResourceNotFoundException;
import com.sharecart.sharecart.invite.dto.AcceptInviteResponse;
import com.sharecart.sharecart.invite.dto.GenerateInviteLinkResponse;
import com.sharecart.sharecart.invite.dto.InvitePreviewResponse;
import com.sharecart.sharecart.invite.model.InviteToken;
import com.sharecart.sharecart.invite.repository.InviteTokenRepository;
import com.sharecart.sharecart.invite.service.InviteService;
import com.sharecart.sharecart.shoppinglist.model.ListMember;
import com.sharecart.sharecart.shoppinglist.model.ShoppingList;
import com.sharecart.sharecart.shoppinglist.repository.ListMemberRepository;
import com.sharecart.sharecart.shoppinglist.repository.ShoppingListRepository;
import com.sharecart.sharecart.user.model.User;
import com.sharecart.sharecart.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InviteServiceImpl implements InviteService {

    private static final long INVITE_EXPIRY_HOURS = 24;

    @Value("${app.invite.base-url}")
    private String inviteBaseUrl;

    private final InviteTokenRepository inviteTokenRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final ListMemberRepository listMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public GenerateInviteLinkResponse generateInviteLink(UUID listId, UUID userId) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping list not found with id: " + listId));

        if (!shoppingListRepository.existsByIdAndOwnerId(listId, userId)) {
            throw new AccessDeniedException("Only the list owner can generate an invite link");
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String rawToken = UUID.randomUUID().toString().replace("-", "");

        InviteToken inviteToken = InviteToken.builder()
                .shoppingList(shoppingList)
                .token(rawToken)
                .role("MEMBER")
                .createdBy(creator)
                .expiresAt(LocalDateTime.now().plusHours(INVITE_EXPIRY_HOURS))
                .build();

        inviteTokenRepository.save(inviteToken);
        log.info("Generated invite link for list {} by user {}", listId, userId);

        return new GenerateInviteLinkResponse(inviteBaseUrl + "/" + rawToken);
    }

    @Override
    @Transactional
    public AcceptInviteResponse acceptInvite(String token, UUID userId) {
        InviteToken inviteToken = inviteTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invite link not found or invalid"));

        if (inviteToken.getExpiresAt() != null && LocalDateTime.now().isAfter(inviteToken.getExpiresAt())) {
            throw new ExpiredInviteTokenException();
        }

        UUID listId = inviteToken.getShoppingList().getId();

        if (listMemberRepository.existsByShoppingListIdAndUserId(listId, userId)) {
            throw new IllegalStateException("You are already a member of this list");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        ListMember member = ListMember.builder()
                .shoppingList(inviteToken.getShoppingList())
                .user(user)
                .role(inviteToken.getRole())
                .build();

        listMemberRepository.save(member);

        inviteToken.setUsed(true);
        inviteTokenRepository.save(inviteToken);

        log.info("User {} joined list {} via invite link", userId, listId);

        return new AcceptInviteResponse(listId, "Joined successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public InvitePreviewResponse getInvitePreview(String token) {
        InviteToken inviteToken = inviteTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invite link not found or invalid"));

        ShoppingList list = inviteToken.getShoppingList();
        String ownerName = list.getOwner() != null ? list.getOwner().getName() : null;

        return new InvitePreviewResponse(list.getName(), ownerName);
    }
}
