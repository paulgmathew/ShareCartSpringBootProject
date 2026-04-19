package com.sharecart.sharecart.shoppinglist.repository;

import com.sharecart.sharecart.shoppinglist.model.ShoppingList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, UUID> {

	List<ShoppingList> findByOwnerId(UUID ownerId);

	boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
