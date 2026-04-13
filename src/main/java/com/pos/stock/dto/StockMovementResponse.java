package com.pos.stock.dto;

import com.pos.stock.entity.StockMovement;
import com.pos.stock.entity.StockMovement.MovementType;

import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        Long itemId,
        String itemName,
        Long shopId,
        String shopName,
        MovementType type,
        Integer quantity,
        Integer balanceBefore,
        Integer balanceAfter,
        String reference,
        String note,
        String createdBy,
        LocalDateTime createdAt
) {
    public static StockMovementResponse from(StockMovement m) {
        return new StockMovementResponse(
                m.getId(),
                m.getItem().getId(),
                m.getItem().getName(),
                m.getShop() != null ? m.getShop().getId()   : null,
                m.getShop() != null ? m.getShop().getName() : null,
                m.getType(),
                m.getQuantity(),
                m.getBalanceBefore(),
                m.getBalanceAfter(),
                m.getReference(),
                m.getNote(),
                m.getCreatedBy() != null ? m.getCreatedBy().getUsername() : null,
                m.getCreatedAt()
        );
    }
}
