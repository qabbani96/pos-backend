package com.pos.transfer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TransferItemRequest(
        @NotNull Long itemId,
        @NotNull @Min(1) Integer quantity
) {}
