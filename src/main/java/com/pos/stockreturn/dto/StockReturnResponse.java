package com.pos.stockreturn.dto;

import com.pos.stockreturn.entity.StockReturn;

import java.time.LocalDateTime;
import java.util.List;

public record StockReturnResponse(
        Long id,
        Long shopId,
        String shopName,
        String status,
        String note,
        String createdBy,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        List<ItemLine> items
) {
    public record ItemLine(Long itemId, String itemName, String itemSku, Integer quantity) {}

    public static StockReturnResponse from(StockReturn r) {
        List<ItemLine> lines = r.getItems().stream()
                .map(i -> new ItemLine(
                        i.getItem().getId(),
                        i.getItem().getName(),
                        i.getItem().getSku(),
                        i.getQuantity()))
                .toList();

        return new StockReturnResponse(
                r.getId(),
                r.getShop().getId(),
                r.getShop().getName(),
                r.getStatus().name(),
                r.getNote(),
                r.getCreatedBy() != null ? r.getCreatedBy().getUsername() : null,
                r.getCompletedAt(),
                r.getCreatedAt(),
                lines
        );
    }
}
