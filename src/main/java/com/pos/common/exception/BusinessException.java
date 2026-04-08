package com.pos.common.exception;

import lombok.Getter;

/**
 * Thrown when a business rule is violated.
 * Example: insufficient stock, duplicate SKU, etc.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    // Common factory methods
    public static BusinessException insufficientStock(String itemName, int available, int requested) {
        return new BusinessException(
                "INSUFFICIENT_STOCK",
                "Insufficient stock for item '" + itemName + "'. Available: " + available + ", Requested: " + requested
        );
    }

    public static BusinessException duplicateSku(String sku) {
        return new BusinessException("DUPLICATE_SKU", "Item with SKU '" + sku + "' already exists");
    }

    public static BusinessException duplicateBarcode(String barcode) {
        return new BusinessException("DUPLICATE_BARCODE", "Barcode '" + barcode + "' is already assigned to another item");
    }
}
