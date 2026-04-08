package com.pos.sale.dto;

import com.pos.sale.entity.Sale;
import com.pos.sale.entity.Sale.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Long id,
        String cashierUsername,
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
                sale.getTotalAmount(),
                sale.getStatus(),
                sale.getNote(),
                items,
                sale.getCreatedAt()
        );
    }
}
