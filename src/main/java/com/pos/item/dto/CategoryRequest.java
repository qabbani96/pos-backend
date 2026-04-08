package com.pos.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        /**
         * ID of the parent category. Null = create a root category (e.g. a Brand).
         * Supply the parent's ID to create a child at any depth.
         */
        Long parentId
) {}
