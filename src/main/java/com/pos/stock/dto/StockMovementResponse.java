package com.pos.stock.dto;

import com.pos.stock.entity.StockMovement;
import com.pos.stock.entity.StockMovement.MovementType;

import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        Long itemId,
        String itemName,
        MovementType type,
        Integer quantity,
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
                m.getType(),
                m.getQuantity(),
                m.getReference(),
                m.getNote(),
                m.getCreatedBy() != null ? m.getCreatedBy().getUsername() : null,
                m.getCreatedAt()
        );
    }
}
