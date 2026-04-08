package com.pos.report.dto;

import java.math.BigDecimal;

public record DashboardSummary(
        long salesToday,
        BigDecimal revenueToday,
        long totalItems,
        long lowStockCount
) {}
