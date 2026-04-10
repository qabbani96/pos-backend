package com.pos.auth.dto;

public record LoginResponse(
        String token,
        String role,
        String fullName,
        Long userId,
        Long branchId   // populated for RECEPTION and CALL_CENTER; null for others
) {}
