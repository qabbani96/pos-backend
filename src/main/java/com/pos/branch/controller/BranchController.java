package com.pos.branch.controller;

import com.pos.branch.dto.BranchRequest;
import com.pos.branch.dto.BranchResponse;
import com.pos.branch.service.BranchService;
import com.pos.common.response.ApiResponse;
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
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Branch management")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'INVENTORY')")
    @Operation(summary = "List all branches")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(branchService.getAllBranches()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES')")
    @Operation(summary = "Get branch by id")
    public ResponseEntity<ApiResponse<BranchResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(branchService.getBranchById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_BRANCHES')")
    @Operation(summary = "Create a new branch")
    public ResponseEntity<ApiResponse<BranchResponse>> create(@Valid @RequestBody BranchRequest request) {
        BranchResponse response = branchService.createBranch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_BRANCHES')")
    @Operation(summary = "Update branch")
    public ResponseEntity<ApiResponse<BranchResponse>> update(
            @PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(branchService.updateBranch(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_BRANCHES')")
    @Operation(summary = "Delete branch")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
