package com.pos.stock.dto;

public record StockSummary(
        long totalItems,
        long lowStockCount
) {}
