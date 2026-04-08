package com.pos.report.controller;

import com.pos.common.response.ApiResponse;
import com.pos.report.dto.CashierReport;
import com.pos.report.dto.DailySalesReport;
import com.pos.report.dto.DashboardSummary;
import com.pos.report.dto.TopItemReport;
import com.pos.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Reports", description = "Dashboard and reporting (Admin only)")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard summary — today's sales, revenue, total items, low stock count")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDashboard()));
    }

    @GetMapping("/sales/daily")
    @Operation(summary = "Daily sales breakdown — revenue and count grouped by day")
    public ResponseEntity<ApiResponse<List<DailySalesReport>>> getDailySales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDailySales(from, to)));
    }

    @GetMapping("/items/top")
    @Operation(summary = "Top-selling items by quantity sold in a date range")
    public ResponseEntity<ApiResponse<List<TopItemReport>>> getTopItems(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getTopItems(from, to, limit)));
    }

    @GetMapping("/cashiers")
    @Operation(summary = "Cashier performance — total sales and revenue per cashier")
    public ResponseEntity<ApiResponse<List<CashierReport>>> getCashierPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getCashierPerformance(from, to)));
    }
}
