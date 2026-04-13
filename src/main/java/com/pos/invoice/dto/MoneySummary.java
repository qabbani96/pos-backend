package com.pos.invoice.dto;

import java.math.BigDecimal;
import java.util.List;

public record MoneySummary(
        BigDecimal           totalAmount,
        long                 invoiceCount,
        List<BranchLine>     breakdown
) {
    public record BranchLine(
            Long       branchId,
            String     branchName,
            BigDecimal totalAmount,
            long       invoiceCount
    ) {}
}
