package com.pos.invoice.dto;

import com.pos.invoice.entity.Invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceResponse(
        Long       id,
        String     invoiceNumber,

        Long       customerId,
        String     customerName,
        String     customerNumber,

        Long       branchId,
        String     branchName,
        String     branchMobile,
        Integer    receiptWidthMm,
        Integer    receiptHeightMm,

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
        String     billTime,

        String        createdByUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static InvoiceResponse from(Invoice i) {
        var branch  = i.getBranch();
        var creator = i.getCreatedBy();
        return new InvoiceResponse(
                i.getId(),
                i.getInvoiceNumber(),

                i.getCustomer() != null ? i.getCustomer().getId()          : null,
                i.getCustomerName(),
                i.getCustomerNumber(),

                branch != null ? branch.getId()                            : null,
                branch != null ? branch.getBranchName()                    : null,
                branch != null ? branch.getMobile()                        : null,
                branch != null ? (int) branch.getReceiptWidthMm()          : 58,
                branch != null ? branch.getReceiptHeightMm()               : null,

                i.getDeviceType(),
                i.getDeviceColor(),
                i.getDeviceQuestion(),
                i.getDeviceStatus() != null ? i.getDeviceStatus().name()   : null,
                i.getDeviceProblem(),
                i.getDeviceImei(),
                i.getDeviceNote(),
                i.getDeviceAccessories(),
                i.getDevicePrice(),
                i.getHiddenPrice(),
                i.getFeedbackCallcenter(),

                i.getEntryDate()      != null ? i.getEntryDate().toString()      : null,
                i.getEntryTime()      != null ? i.getEntryTime().toString()       : null,
                i.getFinishMainDate() != null ? i.getFinishMainDate().toString()  : null,
                i.getFinishMainTime() != null ? i.getFinishMainTime().toString()  : null,
                i.getBillDate()       != null ? i.getBillDate().toString()        : null,
                i.getBillTime()       != null ? i.getBillTime().toString()        : null,

                creator != null ? creator.getUsername() : null,
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }
}
