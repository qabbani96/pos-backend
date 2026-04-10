package com.pos.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        String password,

        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotNull(message = "Role is required")
        String role,     // accepted values: RECEPTION, CALL_CENTER

        Long branchId    // required when role is RECEPTION or CALL_CENTER
) {}
