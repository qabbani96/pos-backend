package com.pos.transfer.controller;

import com.pos.common.response.ApiResponse;
import com.pos.transfer.dto.StockTransferRequest;
import com.pos.transfer.dto.StockTransferResponse;
import com.pos.transfer.service.StockTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock-transfers")
@RequiredArgsConstructor
@Tag(name = "Stock Transfers", description = "Central inventory → shop stock transfers")
public class StockTransferController {

    private final StockTransferService transferService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'INVENTORY')")
    @Operation(summary = "List transfers (optionally filter by shop or status)")
    public ResponseEntity<ApiResponse<Page<StockTransferResponse>>> findAll(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                transferService.findAll(shopId, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'INVENTORY')")
    @Operation(summary = "Get transfer by id with line items")
    public ResponseEntity<ApiResponse<StockTransferResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(transferService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY')")
    @Operation(summary = "Create a new transfer order (status: PENDING)")
    public ResponseEntity<ApiResponse<StockTransferResponse>> create(
            @Valid @RequestBody StockTransferRequest request) {
        StockTransferResponse response = transferService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY')")
    @Operation(summary = "Complete transfer — deducts central stock and adds to shop")
    public ResponseEntity<ApiResponse<StockTransferResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(transferService.complete(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY')")
    @Operation(summary = "Cancel a pending transfer")
    public ResponseEntity<ApiResponse<StockTransferResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(transferService.cancel(id)));
    }
}
