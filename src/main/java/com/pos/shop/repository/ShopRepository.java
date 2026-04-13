package com.pos.shop.repository;

import com.pos.shop.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    boolean existsByName(String name);

    /** True if any user is currently assigned to this shop. */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.shop.id = :shopId")
    boolean hasAssignedUsers(@Param("shopId") Long shopId);

    List<Shop> findAllByActiveTrue();

    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.branch WHERE s.active = true")
    List<Shop> findAllActiveWithBranch();

    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.branch WHERE s.branch.id = :branchId AND s.active = true")
    List<Shop> findActiveByBranchId(Long branchId);
}
