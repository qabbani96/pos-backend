package com.pos.item.controller;

import com.pos.common.response.ApiResponse;
import com.pos.item.dto.CategoryRequest;
import com.pos.item.dto.CategoryResponse;
import com.pos.item.service.CategoryService;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Flexible category hierarchy management")
public class CategoryController {

    private final CategoryService categoryService;

    // ── Read ──────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Flat list of all categories — use for dropdowns")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findAll()));
    }

    @GetMapping("/tree")
    @Operation(summary = "Full category hierarchy as a nested tree — use for admin UI")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findTree() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findTree()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single category by ID (flat)")
    public ResponseEntity<ApiResponse<CategoryResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findById(id)));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Direct children of a category (one level only)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findChildren(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findChildren(id)));
    }

    @GetMapping("/{id}/subtree")
    @Operation(summary = "All descendants of a category (full subtree, flat list)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findSubtree(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findSubtree(id)));
    }

    @GetMapping("/roots")
    @Operation(summary = "Root categories only (level 0 — Brands)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findRoots() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findChildren(null)));
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a category. Set parentId=null for root (Brand), or supply a parentId for sub-levels")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a category — can change name, description, or parent (reparent)")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category — only allowed if it has no children and no items")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
