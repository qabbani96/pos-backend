package com.pos.transfer.dto;

import com.pos.transfer.entity.StockTransfer;
import com.pos.transfer.entity.StockTransferItem;

import java.time.LocalDateTime;
import java.util.List;

public record StockTransferResponse(
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

    public static StockTransferResponse from(StockTransfer t) {
        List<ItemLine> lines = t.getItems().stream()
                .map(i -> new ItemLine(
                        i.getItem().getId(),
                        i.getItem().getName(),
                        i.getItem().getSku(),
                        i.getQuantity()))
                .toList();

        return new StockTransferResponse(
                t.getId(),
                t.getShop().getId(),
                t.getShop().getName(),
                t.getStatus().name(),
                t.getNote(),
                t.getCreatedBy() != null ? t.getCreatedBy().getUsername() : null,
                t.getCompletedAt(),
                t.getCreatedAt(),
                lines
        );
    }
}
