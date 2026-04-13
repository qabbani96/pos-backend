package com.pos.auth.dto;

public record LoginResponse(
        String token,
        String username,
        String role,
        String fullName,
        Long userId,
        Long branchId,  // populated for RECEPTION and CALL_CENTER
        Long shopId,    // populated for CASHIER and INVENTORY
        String shopName
) {}
