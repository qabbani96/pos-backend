package com.pos.report.service;

import com.pos.item.repository.ItemRepository;
import com.pos.report.dto.CashierReport;
import com.pos.report.dto.DailySalesReport;
import com.pos.report.dto.DashboardSummary;
import com.pos.report.dto.TopItemReport;
import com.pos.report.repository.ReportRepository;
import com.pos.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardSummary getDashboard() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(LocalTime.MAX);

        long salesToday   = reportRepository.countSalesToday(startOfDay, endOfDay);
        // SUM returns null when there are no rows — use ZERO as fallback
        BigDecimal revenue = Optional.ofNullable(reportRepository.sumRevenueToday(startOfDay, endOfDay))
                .orElse(BigDecimal.ZERO);
        long totalItems   = itemRepository.count();
        long lowStock     = stockRepository.countLowStock();

        return new DashboardSummary(salesToday, revenue, totalItems, lowStock);
    }

    // ── Daily sales ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DailySalesReport> getDailySales(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return reportRepository.dailySalesRaw(from.atStartOfDay(), to.atTime(LocalTime.MAX))
                .stream()
                .map(row -> new DailySalesReport(
                        ((Date) row[0]).toLocalDate(),          // java.sql.Date → LocalDate
                        ((Number) row[1]).longValue(),           // COUNT(*)
                        new BigDecimal(row[2].toString())        // SUM — safe string conversion
                ))
                .toList();
    }

    // ── Top items ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TopItemReport> getTopItems(LocalDate from, LocalDate to, int limit) {
        validateDateRange(from, to);
        List<TopItemReport> all = reportRepository.topItemsByQuantity(
                from.atStartOfDay(),
                to.atTime(LocalTime.MAX));
        return all.size() > limit ? all.subList(0, limit) : all;
    }

    // ── Cashier performance ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CashierReport> getCashierPerformance(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return reportRepository.cashierPerformance(
                from.atStartOfDay(),
                to.atTime(LocalTime.MAX));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must not be after 'to' date");
        }
    }
}
