package com.pos.stock.controller;

import com.pos.common.response.ApiResponse;
import com.pos.stock.dto.StockAdjustRequest;
import com.pos.stock.dto.StockMovementResponse;
import com.pos.stock.dto.StockResponse;
import com.pos.stock.dto.StockSummary;
import com.pos.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Stock management")
public class StockController {

    private final StockService stockService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all stock levels (paginated, optional low-stock filter)")
    public ResponseEntity<ApiResponse<Page<StockResponse>>> findAll(
            @RequestParam(defaultValue = "false") boolean lowStockOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("item.name").ascending());
        return ResponseEntity.ok(ApiResponse.success(stockService.findAll(lowStockOnly, pageable)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get stock summary (total items, low-stock count)")
    public ResponseEntity<ApiResponse<StockSummary>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(stockService.getSummary()));
    }

    @GetMapping("/item/{itemId}")
    @Operation(summary = "Get stock level for a specific item")
    public ResponseEntity<ApiResponse<StockResponse>> findByItemId(@PathVariable Long itemId) {
        return ResponseEntity.ok(ApiResponse.success(stockService.findByItemId(itemId)));
    }

    @PostMapping("/item/{itemId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adjust stock: IN (add), OUT (deduct), ADJUSTMENT (set absolute)")
    public ResponseEntity<ApiResponse<StockResponse>> adjust(
            @PathVariable Long itemId,
            @Valid @RequestBody StockAdjustRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stockService.adjust(itemId, request)));
    }

    @PatchMapping("/item/{itemId}/min-quantity")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update the minimum stock threshold for low-stock alerting")
    public ResponseEntity<ApiResponse<StockResponse>> updateMinQuantity(
            @PathVariable Long itemId,
            @RequestParam @Min(0) int minQuantity) {
        return ResponseEntity.ok(ApiResponse.success(stockService.updateMinQuantity(itemId, minQuantity)));
    }

    @GetMapping("/item/{itemId}/movements")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get stock movement history for an item")
    public ResponseEntity<ApiResponse<Page<StockMovementResponse>>> getMovements(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(stockService.getMovements(itemId, pageable)));
    }
}
