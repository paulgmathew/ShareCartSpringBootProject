package com.sharecart.sharecart.invite.service;

import com.sharecart.sharecart.invite.dto.AcceptInviteResponse;
import com.sharecart.sharecart.invite.dto.GenerateInviteLinkResponse;
import com.sharecart.sharecart.invite.dto.InvitePreviewResponse;
import java.util.UUID;

public interface InviteService {

    GenerateInviteLinkResponse generateInviteLink(UUID listId, UUID userId);

    AcceptInviteResponse acceptInvite(String token, UUID userId);

    InvitePreviewResponse getInvitePreview(String token);
}
