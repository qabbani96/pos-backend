package com.pos.sale.dto;

import com.pos.sale.entity.Sale;
import com.pos.sale.entity.Sale.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Long id,
        String cashierUsername,
        Long shopId,
        String shopName,
        BigDecimal totalAmount,
        SaleStatus status,
        String note,
        List<SaleItemResponse> items,
        LocalDateTime createdAt
) {
    public static SaleResponse from(Sale sale) {
        List<SaleItemResponse> items = sale.getItems().stream()
                .map(SaleItemResponse::from)
                .toList();

        return new SaleResponse(
                sale.getId(),
                sale.getCashier().getUsername(),
                sale.getShop() != null ? sale.getShop().getId()   : null,
                sale.getShop() != null ? sale.getShop().getName() : null,
                sale.getTotalAmount(),
                sale.getStatus(),
                sale.getNote(),
                items,
                sale.getCreatedAt()
        );
    }
}
