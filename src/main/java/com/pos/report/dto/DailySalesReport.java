package com.pos.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesReport(
        LocalDate date,
        long salesCount,
        BigDecimal totalRevenue
) {}
