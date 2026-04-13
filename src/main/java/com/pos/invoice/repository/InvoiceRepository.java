package com.pos.invoice.repository;

import com.pos.invoice.entity.DeviceStatus;
import com.pos.invoice.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Paginated search with optional branchId, status, and full-text search
     * across invoice number, customer name, and customer number.
     */
    @Query(value = "SELECT i FROM Invoice i "
            + "LEFT JOIN FETCH i.branch b "
            + "LEFT JOIN FETCH i.createdBy u "
            + "WHERE (:branchId IS NULL OR b.id           = :branchId) "
            + "  AND (:status   IS NULL OR i.deviceStatus  = :status) "
            + "  AND (:search   IS NULL "
            + "       OR LOWER(i.invoiceNumber)  LIKE LOWER(CONCAT('%', :search, '%')) "
            + "       OR LOWER(i.customerName)   LIKE LOWER(CONCAT('%', :search, '%')) "
            + "       OR LOWER(i.customerNumber) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "ORDER BY i.createdAt DESC",
            countQuery = "SELECT COUNT(i) FROM Invoice i "
            + "LEFT JOIN i.branch b "
            + "WHERE (:branchId IS NULL OR b.id          = :branchId) "
            + "  AND (:status   IS NULL OR i.deviceStatus = :status) "
            + "  AND (:search   IS NULL "
            + "       OR LOWER(i.invoiceNumber)  LIKE LOWER(CONCAT('%', :search, '%')) "
            + "       OR LOWER(i.customerName)   LIKE LOWER(CONCAT('%', :search, '%')) "
            + "       OR LOWER(i.customerNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Invoice> search(
            @Param("branchId") Long branchId,
            @Param("status")   DeviceStatus status,
            @Param("search")   String search,
            Pageable pageable
    );

    // ── Money aggregation ─────────────────────────────────────────────────────

    /** Total device_price and invoice count, filtered by optional branch and status. */
    @Query("SELECT COALESCE(SUM(i.devicePrice), 0), COUNT(i) "
            + "FROM Invoice i "
            + "LEFT JOIN i.branch b "
            + "WHERE (:branchId IS NULL OR b.id          = :branchId) "
            + "  AND (:status   IS NULL OR i.deviceStatus = :status)")
    Object[] sumAndCount(
            @Param("branchId") Long branchId,
            @Param("status")   DeviceStatus status
    );

    /** Per-branch breakdown: [branchId, branchName, totalAmount, invoiceCount]. */
    @Query("SELECT b.id, b.branchName, COALESCE(SUM(i.devicePrice), 0), COUNT(i) "
            + "FROM Invoice i "
            + "JOIN i.branch b "
            + "WHERE (:status IS NULL OR i.deviceStatus = :status) "
            + "GROUP BY b.id, b.branchName "
            + "ORDER BY SUM(i.devicePrice) DESC")
    List<Object[]> breakdownByBranch(@Param("status") DeviceStatus status);

    // ── Customer invoices ─────────────────────────────────────────────────────

    @Query(value = "SELECT i FROM Invoice i "
            + "LEFT JOIN FETCH i.branch "
            + "LEFT JOIN FETCH i.createdBy "
            + "WHERE i.customer.id = :customerId "
            + "ORDER BY i.createdAt DESC",
            countQuery = "SELECT COUNT(i) FROM Invoice i WHERE i.customer.id = :customerId")
    org.springframework.data.domain.Page<Invoice> findByCustomerId(
            @Param("customerId") Long customerId,
            org.springframework.data.domain.Pageable pageable);
}
