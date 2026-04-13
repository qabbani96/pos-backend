package com.pos.item.controller;

import com.pos.common.response.ApiResponse;
import com.pos.item.dto.ItemRequest;
import com.pos.item.dto.ItemResponse;
import com.pos.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
@Tag(name = "Items", description = "Item management")
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    @Operation(summary = "List items (paginated, filterable)")
    public ResponseEntity<ApiResponse<Page<ItemResponse>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(ApiResponse.success(
                itemService.findAll(search, categoryId, activeOnly, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID")
    public ResponseEntity<ApiResponse<ItemResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(itemService.findById(id)));
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Look up item by barcode (used by Android POS)")
    public ResponseEntity<ApiResponse<ItemResponse>> findByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(ApiResponse.success(itemService.findByBarcode(barcode)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new item")
    public ResponseEntity<ApiResponse<ItemResponse>> create(
            @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(itemService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an item")
    public ResponseEntity<ApiResponse<ItemResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(itemService.update(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate an item (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        itemService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-activate an item")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        itemService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
