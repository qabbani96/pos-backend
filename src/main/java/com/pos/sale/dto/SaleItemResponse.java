package com.pos.sale.dto;

import com.pos.sale.entity.SaleItem;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long itemId,
        String itemName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public static SaleItemResponse from(SaleItem si) {
        return new SaleItemResponse(
                si.getItem().getId(),
                si.getItemName(),
                si.getQuantity(),
                si.getUnitPrice(),
                si.getSubtotal()
        );
    }
}
