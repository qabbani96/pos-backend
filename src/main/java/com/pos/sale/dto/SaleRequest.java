package com.pos.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaleRequest(

        @NotEmpty(message = "Sale must contain at least one item")
        @Valid
        List<SaleItemRequest> items,

        @Size(max = 255, message = "Note must not exceed 255 characters")
        String note
) {}
