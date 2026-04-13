package com.pos.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(

        @NotBlank
        @Size(max = 100)
        String customerName,

        @NotBlank
        @Size(max = 20)
        String customerNumber
) {}
