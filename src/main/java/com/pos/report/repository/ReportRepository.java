package com.pos.report.repository;

import com.pos.report.dto.CashierReport;
import com.pos.report.dto.TopItemReport;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only reporting repository — no entity, no writes.
 * Uses JPQL aggregate queries across Sale and SaleItem.
 */
public interface ReportRepository extends Repository<com.pos.sale.entity.Sale, Long> {

    // ── Today's totals (for dashboard) ───────────────────────────────────────

    @Query("""
            SELECT COUNT(s) FROM Sale s
            WHERE s.status = 'COMPLETED'
              AND s.createdAt >= :startOfDay
              AND s.createdAt <  :endOfDay
            """)
    long countSalesToday(@Param("startOfDay") LocalDateTime startOfDay,
                         @Param("endOfDay")   LocalDateTime endOfDay);

    /**
     * Returns NULL (not 0) when there are no sales — handled by the service.
     * COALESCE(SUM, 0) returns Integer when SUM is null, causing ClassCastException.
     */
    @Query("""
            SELECT SUM(s.totalAmount) FROM Sale s
            WHERE s.status = 'COMPLETED'
              AND s.createdAt >= :startOfDay
              AND s.createdAt <  :endOfDay
            """)
    BigDecimal sumRevenueToday(@Param("startOfDay") LocalDateTime startOfDay,
                               @Param("endOfDay")   LocalDateTime endOfDay);

    // ── Daily revenue breakdown ───────────────────────────────────────────────

    /**
     * Native SQL — avoids JPQL CAST/constructor-expression type mapping issues
     * with java.sql.Date vs java.time.LocalDate.
     * Returns Object[]{java.sql.Date, Long, BigDecimal} per row.
     */
    @Query(value = """
            SELECT DATE(s.created_at)        AS sale_date,
                   COUNT(*)                  AS sales_count,
                   COALESCE(SUM(s.total_amount), 0) AS total_revenue
            FROM   sales s
            WHERE  s.status = 'COMPLETED'
              AND  s.created_at >= :from
              AND  s.created_at <= :to
            GROUP  BY DATE(s.created_at)
            ORDER  BY DATE(s.created_at) ASC
            """, nativeQuery = true)
    List<Object[]> dailySalesRaw(@Param("from") LocalDateTime from,
                                 @Param("to")   LocalDateTime to);

    // ── Top-selling items ─────────────────────────────────────────────────────

    @Query("""
            SELECT new com.pos.report.dto.TopItemReport(
                si.item.id,
                si.itemName,
                SUM(si.quantity),
                SUM(si.subtotal)
            )
            FROM SaleItem si
            JOIN si.sale s
            WHERE s.status = 'COMPLETED'
              AND s.createdAt >= :from
              AND s.createdAt <= :to
            GROUP BY si.item.id, si.itemName
            ORDER BY SUM(si.quantity) DESC
            """)
    List<TopItemReport> topItemsByQuantity(@Param("from") LocalDateTime from,
                                           @Param("to")   LocalDateTime to);

    // ── Cashier performance ───────────────────────────────────────────────────

    @Query("""
            SELECT new com.pos.report.dto.CashierReport(
                s.cashier.id,
                s.cashier.username,
                COUNT(s),
                COALESCE(SUM(s.totalAmount), 0)
            )
            FROM Sale s
            WHERE s.status = 'COMPLETED'
              AND s.createdAt >= :from
              AND s.createdAt <= :to
            GROUP BY s.cashier.id, s.cashier.username
            ORDER BY SUM(s.totalAmount) DESC
            """)
    List<CashierReport> cashierPerformance(@Param("from") LocalDateTime from,
                                           @Param("to")   LocalDateTime to);
}
