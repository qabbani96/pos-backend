package com.pos.stock.repository;

import com.pos.stock.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query(value = """
            SELECT m FROM StockMovement m
            LEFT JOIN FETCH m.createdBy
            WHERE m.item.id = :itemId
            """,
            countQuery = "SELECT COUNT(m) FROM StockMovement m WHERE m.item.id = :itemId")
    Page<StockMovement> findByItemId(@Param("itemId") Long itemId, Pageable pageable);
}
