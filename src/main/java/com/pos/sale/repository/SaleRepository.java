package com.pos.sale.repository;

import com.pos.sale.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query(value = """
            SELECT s FROM Sale s
            JOIN FETCH s.cashier
            WHERE (:cashierId IS NULL OR s.cashier.id = :cashierId)
              AND (:from IS NULL OR s.createdAt >= :from)
              AND (:to   IS NULL OR s.createdAt <= :to)
            """,
            countQuery = """
            SELECT COUNT(s) FROM Sale s
            WHERE (:cashierId IS NULL OR s.cashier.id = :cashierId)
              AND (:from IS NULL OR s.createdAt >= :from)
              AND (:to   IS NULL OR s.createdAt <= :to)
            """)
    Page<Sale> findAll(
            @Param("cashierId") Long cashierId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
