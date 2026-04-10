package com.sharecart.sharecart.invite.controller;

import com.sharecart.sharecart.invite.dto.AcceptInviteResponse;
import com.sharecart.sharecart.invite.dto.GenerateInviteLinkResponse;
import com.sharecart.sharecart.invite.dto.InvitePreviewResponse;
import com.sharecart.sharecart.invite.service.InviteService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    // POST /api/v1/lists/{listId}/invite-link
    @PostMapping("/lists/{listId}/invite-link")
    public ResponseEntity<GenerateInviteLinkResponse> generateInviteLink(@PathVariable UUID listId) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(inviteService.generateInviteLink(listId, UUID.fromString(userId)));
    }

    // POST /api/v1/invites/{token}/accept
    @PostMapping("/invites/{token}/accept")
    public ResponseEntity<AcceptInviteResponse> acceptInvite(@PathVariable String token) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(inviteService.acceptInvite(token, UUID.fromString(userId)));
    }

    // GET /api/v1/invites/{token}  — public, no auth required
    @GetMapping("/invites/{token}")
    public ResponseEntity<InvitePreviewResponse> getInvitePreview(@PathVariable String token) {
        return ResponseEntity.ok(inviteService.getInvitePreview(token));
    }
}
