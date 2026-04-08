package com.pos.stock.repository;

import com.pos.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
