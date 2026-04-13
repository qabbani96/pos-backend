package com.pos.item.dto;

import com.pos.item.entity.Item.BarcodeType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ItemRequest(

        @NotBlank(message = "SKU is required")
        @Size(max = 50, message = "SKU must not exceed 50 characters")
        String sku,

        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        String description,

        Long categoryId,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Price format invalid")
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "Cost price must be >= 0")
        @Digits(integer = 8, fraction = 2, message = "Cost price format invalid")
        BigDecimal costPrice,

        @Size(max = 100, message = "Barcode must not exceed 100 characters")
        String barcode,

        BarcodeType barcodeType,

        @Size(max = 255, message = "Image URL must not exceed 255 characters")
        String imageUrl
) {}
