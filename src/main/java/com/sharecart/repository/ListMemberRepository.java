package com.sharecart.repository;

import com.sharecart.entity.ListMember;
import com.sharecart.entity.ShoppingList;
import com.sharecart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListMemberRepository extends JpaRepository<ListMember, Long> {
    boolean existsByListAndUser(ShoppingList list, User user);
}
