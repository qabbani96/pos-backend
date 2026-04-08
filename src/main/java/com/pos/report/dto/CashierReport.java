package com.pos.report.dto;

import java.math.BigDecimal;

public record CashierReport(
        Long cashierId,
        String cashierUsername,
        long totalSales,
        BigDecimal totalRevenue
) {}
