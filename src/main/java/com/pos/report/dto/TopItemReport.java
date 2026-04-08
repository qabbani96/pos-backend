package com.pos.report.dto;

import java.math.BigDecimal;

public record TopItemReport(
        Long itemId,
        String itemName,
        long totalQuantitySold,
        BigDecimal totalRevenue
) {}
