package com.pos.invoice.dto;

import java.math.BigDecimal;

public record InvoiceRequest(
        Long       branchId,
        String     customerName,
        String     customerNumber,
        String     deviceType,
        String     deviceColor,
        String     deviceQuestion,
        String     deviceStatus,
        String     deviceProblem,
        String     deviceImei,
        String     deviceNote,
        String     deviceAccessories,
        BigDecimal devicePrice,
        BigDecimal hiddenPrice,
        String     feedbackCallcenter,
        String     entryDate,
        String     entryTime,
        String     finishMainDate,
        String     finishMainTime,
        String     billDate,
        String     billTime
) {}
