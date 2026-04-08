package com.pos.auth.dto;

public record LoginResponse(
        String token,
        String role,
        String fullName,
        Long userId
) {}
