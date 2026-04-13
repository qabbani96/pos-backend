package com.pos.transfer.repository;

import com.pos.transfer.entity.StockTransfer;
import com.pos.transfer.entity.StockTransfer.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    /**
     * List transfers with optional shop/status filter.
     *
     * IMPORTANT: countQuery must be provided separately because Hibernate cannot
     * automatically derive a count query from a JPQL query that contains JOIN FETCH.
     * Without countQuery, Hibernate throws:
     *   "query specified join fetching, but the owner of the fetched association
     *    was not present in the select list"
     */
    @Query(value = """
            SELECT t FROM StockTransfer t
            JOIN FETCH t.shop
            LEFT JOIN FETCH t.createdBy
            WHERE (:shopId IS NULL OR t.shop.id = :shopId)
              AND (:status IS NULL OR t.status = :status)
            ORDER BY t.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(t) FROM StockTransfer t
            WHERE (:shopId IS NULL OR t.shop.id = :shopId)
              AND (:status IS NULL OR t.status = :status)
            """)
    Page<StockTransfer> findAll(@Param("shopId") Long shopId,
                                 @Param("status") TransferStatus status,
                                 Pageable pageable);

    @Query("SELECT t FROM StockTransfer t JOIN FETCH t.shop JOIN FETCH t.items ti JOIN FETCH ti.item WHERE t.id = :id")
    Optional<StockTransfer> findByIdWithItems(@Param("id") Long id);
}
