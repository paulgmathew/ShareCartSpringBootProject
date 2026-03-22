package com.sharecart.sharecart.item.repository;

import com.sharecart.sharecart.item.model.Item;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    List<Item> findByShoppingListId(UUID listId);
}
