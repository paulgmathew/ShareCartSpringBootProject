package com.sharecart.sharecart.shoppinglist.repository;

import com.sharecart.sharecart.shoppinglist.model.ListMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListMemberRepository extends JpaRepository<ListMember, UUID> {

    List<ListMember> findByShoppingListId(UUID listId);

    List<ListMember> findByUserId(UUID userId);

    Optional<ListMember> findByShoppingListIdAndUserId(UUID listId, UUID userId);
}
