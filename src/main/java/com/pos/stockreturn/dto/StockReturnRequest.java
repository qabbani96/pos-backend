package com.pos.stockreturn.dto;

import com.pos.transfer.dto.TransferItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StockReturnRequest(
        @NotNull Long shopId,
        String note,
        @NotEmpty @Valid List<TransferItemRequest> items
) {}
