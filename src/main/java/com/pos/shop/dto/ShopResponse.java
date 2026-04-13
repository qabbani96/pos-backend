package com.pos.shop.dto;

import com.pos.shop.entity.Shop;

import java.time.LocalDateTime;

public record ShopResponse(
        Long id,
        String name,
        Long branchId,
        String branchName,
        boolean active,
        LocalDateTime createdAt
) {
    public static ShopResponse from(Shop shop) {
        return new ShopResponse(
                shop.getId(),
                shop.getName(),
                shop.getBranch() != null ? shop.getBranch().getId()         : null,
                shop.getBranch() != null ? shop.getBranch().getBranchName() : null,
                shop.isActive(),
                shop.getCreatedAt()
        );
    }
}
