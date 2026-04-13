package com.pos.shopstock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ShopStockAdjustRequest(
        @NotNull @Min(0) Integer quantity,
        String note
) {}
