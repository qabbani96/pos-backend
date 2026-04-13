package com.pos.auth.dto;

import jakarta.validation.constraints.Size;

/**
 * All fields are optional — only non-null fields are applied on update.
 * password is only changed when explicitly provided.
 */
public record UpdateUserRequest(

        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        String password,   // null = keep existing password

        String role,       // null = keep existing role; allowed: RECEPTION, CALL_CENTER

        Boolean active,    // null = keep existing status

        Long branchId,     // null = keep existing branch; use -1 to explicitly remove branch

        Long shopId        // null = keep existing shop; use -1 to explicitly remove shop
) {}
