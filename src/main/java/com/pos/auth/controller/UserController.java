package com.pos.auth.controller;

import com.pos.auth.dto.CreateUserRequest;
import com.pos.auth.dto.UpdateUserRequest;
import com.pos.auth.dto.UserResponse;
import com.pos.auth.service.UserService;
import com.pos.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin-only user management (RECEPTION and CALL_CENTER roles)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")   // all endpoints in this controller require ADMIN
public class UserController {

    private final UserService userService;

    // ── GET all ──────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users fetched", userService.getAllUsers()));
    }

    // ── GET by id ────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User fetched", userService.getUserById(id)));
    }

    // ── POST create ──────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
            summary = "Create user",
            description = "Creates a new user. Allowed roles: RECEPTION, CALL_CENTER."
    )
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", response));
    }

    // ── PUT update ───────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(
            summary = "Update user",
            description = "Updates fullName, password, role, or active status. " +
                          "Only non-null fields are applied. Allowed roles: RECEPTION, CALL_CENTER."
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userService.updateUser(id, request)));
    }

    // ── PATCH activate / deactivate ──────────────────────────────────────────

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate user account")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User activated", userService.activateUser(id)));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user account")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User deactivated", userService.deactivateUser(id)));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete user",
            description = "Permanently deletes a user. Admin accounts cannot be deleted."
    )
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}
