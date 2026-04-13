package com.pos.shopstock.dto;

import com.pos.shopstock.entity.ShopStock;

public record ShopStockResponse(
        Long id,
        Long shopId,
        String shopName,
        Long itemId,
        String itemName,
        String itemSku,
        String barcode,
        Integer quantity,
        Integer minQuantity,
        boolean low
) {
    public static ShopStockResponse from(ShopStock ss) {
        return new ShopStockResponse(
                ss.getId(),
                ss.getShop().getId(),
                ss.getShop().getName(),
                ss.getItem().getId(),
                ss.getItem().getName(),
                ss.getItem().getSku(),
                ss.getItem().getBarcode(),
                ss.getQuantity(),
                ss.getMinQuantity(),
                ss.isLow()
        );
    }
}
