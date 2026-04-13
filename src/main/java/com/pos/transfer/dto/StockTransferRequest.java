package com.pos.transfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StockTransferRequest(
        @NotNull Long shopId,
        String note,
        @NotEmpty @Valid List<TransferItemRequest> items
) {}
