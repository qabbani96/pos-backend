package com.pos.shopstock.controller;

import com.pos.common.response.ApiResponse;
import com.pos.shopstock.dto.ShopStockAdjustRequest;
import com.pos.shopstock.dto.ShopStockResponse;
import com.pos.shopstock.service.ShopStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shops/{shopId}/stock")
@RequiredArgsConstructor
@Tag(name = "Shop Stock", description = "Per-shop item stock levels")
public class ShopStockController {

    private final ShopStockService shopStockService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'INVENTORY', 'CASHIER')")
    @Operation(summary = "List stock for a shop")
    public ResponseEntity<ApiResponse<Page<ShopStockResponse>>> findByShop(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "false") boolean lowStockOnly,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                shopStockService.findByShop(shopId, lowStockOnly, search, pageable)));
    }

    @GetMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'INVENTORY', 'CASHIER')")
    @Operation(summary = "Get stock for a specific item in a shop")
    public ResponseEntity<ApiResponse<ShopStockResponse>> findByShopAndItem(
            @PathVariable Long shopId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(ApiResponse.success(
                shopStockService.findByShopAndItem(shopId, itemId)));
    }

    @PatchMapping("/items/{itemId}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY')")
    @Operation(summary = "Manually adjust shop stock quantity")
    public ResponseEntity<ApiResponse<ShopStockResponse>> adjust(
            @PathVariable Long shopId,
            @PathVariable Long itemId,
            @Valid @RequestBody ShopStockAdjustRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                shopStockService.adjust(shopId, itemId, request)));
    }
}
