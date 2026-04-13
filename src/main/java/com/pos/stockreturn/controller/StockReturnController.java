package com.pos.stockreturn.controller;

import com.pos.common.response.ApiResponse;
import com.pos.stockreturn.dto.StockReturnRequest;
import com.pos.stockreturn.dto.StockReturnResponse;
import com.pos.stockreturn.service.StockReturnService;
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
@RequestMapping("/api/v1/stock-returns")
@RequiredArgsConstructor
@Tag(name = "Stock Returns", description = "Shop → central inventory stock returns")
public class StockReturnController {

    private final StockReturnService returnService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'INVENTORY')")
    @Operation(summary = "List returns (optionally filter by shop or status)")
    public ResponseEntity<ApiResponse<Page<StockReturnResponse>>> findAll(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                returnService.findAll(shopId, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'INVENTORY')")
    @Operation(summary = "Get return by id with line items")
    public ResponseEntity<ApiResponse<StockReturnResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(returnService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY')")
    @Operation(summary = "Create a return order (status: PENDING)")
    public ResponseEntity<ApiResponse<StockReturnResponse>> create(
            @Valid @RequestBody StockReturnRequest request) {
        StockReturnResponse response = returnService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY')")
    @Operation(summary = "Complete return — deducts shop stock and adds to central")
    public ResponseEntity<ApiResponse<StockReturnResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(returnService.complete(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY')")
    @Operation(summary = "Cancel a pending return")
    public ResponseEntity<ApiResponse<StockReturnResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(returnService.cancel(id)));
    }
}
