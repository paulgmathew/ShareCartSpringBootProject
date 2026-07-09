package com.sharecart.sharecart.invite.controller;

import com.sharecart.sharecart.invite.dto.AcceptInviteResponse;
import com.sharecart.sharecart.invite.dto.GenerateInviteLinkResponse;
import com.sharecart.sharecart.invite.dto.InvitePreviewResponse;
import com.sharecart.sharecart.invite.service.InviteService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    // POST /api/v1/lists/{listId}/invite-link
    @PostMapping("/lists/{listId}/invite-link")
    public ResponseEntity<GenerateInviteLinkResponse> generateInviteLink(@PathVariable UUID listId) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("POST /api/v1/lists/{}/invite-link userId={}", listId, userId);
        GenerateInviteLinkResponse response = inviteService.generateInviteLink(listId, UUID.fromString(userId));
        log.info("Invite link generated listId={} userId={}", listId, userId);
        return ResponseEntity.ok(response);
    }

    // POST /api/v1/invites/{token}/accept
    @PostMapping("/invites/{token}/accept")
    public ResponseEntity<AcceptInviteResponse> acceptInvite(@PathVariable String token) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("POST /api/v1/invites/{token}/accept userId={}", userId);
        AcceptInviteResponse response = inviteService.acceptInvite(token, UUID.fromString(userId));
        log.info("Invite accepted userId={} listId={}", userId, response.listId());
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/invites/{token}  — public, no auth required
    @GetMapping("/invites/{token}")
    public ResponseEntity<InvitePreviewResponse> getInvitePreview(@PathVariable String token) {
        log.info("GET /api/v1/invites/{token} (public)");
        return ResponseEntity.ok(inviteService.getInvitePreview(token));
    }
}
