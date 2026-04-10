package com.pos.auth.dto;

import com.pos.auth.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String role,
        boolean active,
        Long branchId,
        String branchName,
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
                user.getBranch() != null ? user.getBranch().getId()         : null,
                user.getBranch() != null ? user.getBranch().getBranchName() : null,
                user.getCreatedAt()
        );
    }
}
