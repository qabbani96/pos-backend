package com.pos.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShopRequest(
        @NotBlank @Size(max = 100) String name,
        Long branchId
) {}
