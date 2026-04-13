package com.pos.branch.dto;

import com.pos.branch.entity.Branch;

import java.time.LocalDateTime;

public record BranchResponse(
        Long          id,
        String        branchName,
        String        mobile,
        int           receiptWidthMm,
        Integer       receiptHeightMm,     // null = auto height
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getBranchName(),
                branch.getMobile(),
                branch.getReceiptWidthMm(),
                branch.getReceiptHeightMm(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }
}
