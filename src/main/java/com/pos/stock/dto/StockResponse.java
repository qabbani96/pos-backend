package com.pos.stock.dto;

import com.pos.stock.entity.Stock;

import java.time.LocalDateTime;

public record StockResponse(
        Long stockId,
        Long itemId,
        String itemSku,
        String itemName,
        Integer quantity,
        Integer minQuantity,
        boolean low,
        LocalDateTime updatedAt
) {
    public static StockResponse from(Stock stock) {
        return new StockResponse(
                stock.getId(),
                stock.getItem().getId(),
                stock.getItem().getSku(),
                stock.getItem().getName(),
                stock.getQuantity(),
                stock.getMinQuantity(),
                stock.isLow(),
                stock.getUpdatedAt()
        );
    }
}
