package com.pos.stockreturn.repository;

import com.pos.stockreturn.entity.StockReturn;
import com.pos.stockreturn.entity.StockReturn.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockReturnRepository extends JpaRepository<StockReturn, Long> {

    /**
     * List returns with optional shop/status filter.
     *
     * countQuery is required — Hibernate cannot automatically derive it from a
     * query containing JOIN FETCH. Omitting it causes HibernateException at runtime.
     */
    @Query(value = """
            SELECT r FROM StockReturn r
            JOIN FETCH r.shop
            LEFT JOIN FETCH r.createdBy
            WHERE (:shopId IS NULL OR r.shop.id = :shopId)
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(r) FROM StockReturn r
            WHERE (:shopId IS NULL OR r.shop.id = :shopId)
              AND (:status IS NULL OR r.status = :status)
            """)
    Page<StockReturn> findAll(@Param("shopId") Long shopId,
                               @Param("status") ReturnStatus status,
                               Pageable pageable);

    @Query("SELECT r FROM StockReturn r JOIN FETCH r.shop JOIN FETCH r.items ri JOIN FETCH ri.item WHERE r.id = :id")
    Optional<StockReturn> findByIdWithItems(@Param("id") Long id);
}
