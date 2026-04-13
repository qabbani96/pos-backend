package com.pos.stock.dto;

import com.pos.stock.entity.StockMovement.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustRequest(

        @NotNull(message = "Movement type is required")
        MovementType type,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @Size(max = 100, message = "Reference must not exceed 100 characters")
        String reference,

        @Size(max = 255, message = "Note must not exceed 255 characters")
        String note
) {}
