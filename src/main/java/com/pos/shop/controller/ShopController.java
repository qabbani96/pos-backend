package com.pos.shop.controller;

import com.pos.common.response.ApiResponse;
import com.pos.shop.dto.ShopRequest;
import com.pos.shop.dto.ShopResponse;
import com.pos.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@Tag(name = "Shops", description = "Shop management (physical POS locations)")
public class ShopController {

    private final ShopService shopService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'INVENTORY')")
    @Operation(summary = "List all shops")
    public ResponseEntity<ApiResponse<List<ShopResponse>>> findAll(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(shopService.findAll(activeOnly)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'INVENTORY')")
    @Operation(summary = "Get shop by id")
    public ResponseEntity<ApiResponse<ShopResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(shopService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES')")
    @Operation(summary = "Create a new shop")
    public ResponseEntity<ApiResponse<ShopResponse>> create(@Valid @RequestBody ShopRequest request) {
        ShopResponse response = shopService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES')")
    @Operation(summary = "Update shop")
    public ResponseEntity<ApiResponse<ShopResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ShopRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shopService.update(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES')")
    @Operation(summary = "Deactivate shop")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        shopService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES')")
    @Operation(summary = "Activate shop")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        shopService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES')")
    @Operation(summary = "Delete shop",
               description = "Permanently deletes a shop. Fails if the shop has assigned users or stock entries.")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        shopService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
