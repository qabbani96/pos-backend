package com.pos.shopstock.repository;

import com.pos.shopstock.entity.ShopStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShopStockRepository extends JpaRepository<ShopStock, Long> {

    @Query("SELECT ss FROM ShopStock ss JOIN FETCH ss.item WHERE ss.shop.id = :shopId AND ss.item.id = :itemId")
    Optional<ShopStock> findByShopIdAndItemId(@Param("shopId") Long shopId,
                                               @Param("itemId") Long itemId);

    @Query(value = """
            SELECT ss FROM ShopStock ss
            JOIN FETCH ss.item i
            JOIN FETCH ss.shop s
            WHERE ss.shop.id = :shopId
              AND (:lowStockOnly = false OR ss.quantity <= ss.minQuantity)
              AND (:search IS NULL
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.sku)  LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(ss) FROM ShopStock ss
            JOIN ss.item i
            WHERE ss.shop.id = :shopId
              AND (:lowStockOnly = false OR ss.quantity <= ss.minQuantity)
              AND (:search IS NULL
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.sku)  LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ShopStock> findByShop(@Param("shopId") Long shopId,
                                @Param("lowStockOnly") boolean lowStockOnly,
                                @Param("search") String search,
                                Pageable pageable);

    /** Batch-load quantities for a set of items in a specific shop. */
    @Query("SELECT ss.item.id, ss.quantity FROM ShopStock ss WHERE ss.shop.id = :shopId AND ss.item.id IN :itemIds")
    List<Object[]> findQuantitiesByShopAndItemIds(@Param("shopId") Long shopId,
                                                   @Param("itemIds") Collection<Long> itemIds);

    @Query("SELECT COUNT(ss) FROM ShopStock ss WHERE ss.shop.id = :shopId AND ss.quantity <= ss.minQuantity")
    long countLowStockByShop(@Param("shopId") Long shopId);

    /** Check if a shop stock record already exists for an item. */
    boolean existsByShopIdAndItemId(Long shopId, Long itemId);

    /** True if the shop has ANY stock records (regardless of quantity). */
    boolean existsByShopId(Long shopId);
}
