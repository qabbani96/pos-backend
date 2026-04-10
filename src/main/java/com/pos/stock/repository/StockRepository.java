package com.pos.stock.repository;

import com.pos.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    @Query("SELECT s FROM Stock s JOIN FETCH s.item WHERE s.item.id = :itemId")
    Optional<Stock> findByItemId(Long itemId);

    @Query(value = """
            SELECT s FROM Stock s
            JOIN FETCH s.item i
            WHERE i.active = true
              AND (:lowStockOnly = false OR s.quantity <= s.minQuantity)
            """,
            countQuery = """
            SELECT COUNT(s) FROM Stock s
            JOIN s.item i
            WHERE i.active = true
              AND (:lowStockOnly = false OR s.quantity <= s.minQuantity)
            """)
    Page<Stock> findAll(boolean lowStockOnly, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Stock s WHERE s.quantity <= s.minQuantity AND s.item.active = true")
    long countLowStock();

    /**
     * Batch-load [itemId, quantity] pairs for a set of item IDs in one query.
     * Returns Object[] rows: index 0 = itemId (Long), index 1 = quantity (Integer).
     * Avoids lazy-loading the Item association entirely.
     */
    @Query("SELECT s.item.id, s.quantity FROM Stock s WHERE s.item.id IN :itemIds")
    List<Object[]> findQuantitiesByItemIds(@Param("itemIds") Collection<Long> itemIds);
}
