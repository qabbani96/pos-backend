package com.pos.auth.dto;

import com.pos.auth.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String role,
        boolean active,
        LocalDateTime createdAt
) {
    /** Convenience factory — maps directly from entity. */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
