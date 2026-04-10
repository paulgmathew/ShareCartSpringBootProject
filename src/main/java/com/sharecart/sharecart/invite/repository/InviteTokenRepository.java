package com.sharecart.sharecart.invite.repository;

import com.sharecart.sharecart.invite.model.InviteToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteTokenRepository extends JpaRepository<InviteToken, UUID> {

    Optional<InviteToken> findByToken(String token);

    List<InviteToken> findByShoppingListId(UUID listId);
}
