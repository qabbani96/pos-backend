package com.pos.branch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BranchRequest(

        @NotBlank(message = "Branch name is required")
        @Size(min = 2, max = 100, message = "Branch name must be between 2 and 100 characters")
        String branchName,

        @Size(max = 20, message = "Mobile number must be 20 characters or fewer")
        String mobile,

        /** Thermal paper width in mm. Accepts 58 or 80. Defaults to 58 if null. */
        Integer receiptWidthMm,

        /** Thermal paper height in mm. Null = auto (content-driven). */
        Integer receiptHeightMm
) {}
