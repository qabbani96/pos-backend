package com.pos.sale.controller;

import com.pos.common.response.ApiResponse;
import com.pos.sale.dto.SaleRequest;
import com.pos.sale.dto.SaleResponse;
import com.pos.sale.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@Tag(name = "Sales", description = "Sale processing and history")
public class SaleController {

    private final SaleService saleService;

    /**
     * Core endpoint — called by the Android POS after the cashier confirms the cart.
     * Accessible by both CASHIER and ADMIN roles.
     */
    @PostMapping
    @Operation(summary = "Process a sale (atomic: validate stock → save → deduct)")
    public ResponseEntity<ApiResponse<SaleResponse>> processSale(
            @Valid @RequestBody SaleRequest request) {
        SaleResponse sale = saleService.processSale(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sale completed successfully", sale));
    }

    /**
     * Receipt lookup — used by Android to re-display or reprint a receipt.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get sale by ID (receipt)")
    public ResponseEntity<ApiResponse<SaleResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(saleService.findById(id)));
    }

    /**
     * Sales history — ADMIN only, filterable by cashier and date range.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List sales (admin only) — filter by cashier, date range")
    public ResponseEntity<ApiResponse<Page<SaleResponse>>> findAll(
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                saleService.findAll(cashierId, from, to, pageable)));
    }
}
