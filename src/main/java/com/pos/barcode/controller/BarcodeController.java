package com.pos.barcode.controller;

import com.pos.barcode.service.BarcodeService;
import com.pos.common.response.ApiResponse;
import com.pos.item.dto.ItemResponse;
import com.pos.item.entity.Item;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/barcodes")
@RequiredArgsConstructor
@Tag(name = "Barcodes", description = "Barcode generation and label printing")
public class BarcodeController {

    private final BarcodeService barcodeService;

    /**
     * Auto-generates a barcode value for the item and saves it.
     * Safe to call multiple times — idempotent if barcode already exists.
     */
    @PostMapping("/item/{itemId}/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate and assign a barcode to an item (idempotent)")
    public ResponseEntity<ApiResponse<ItemResponse>> generate(@PathVariable Long itemId) {
        Item item = barcodeService.assignBarcode(itemId);
        return ResponseEntity.ok(ApiResponse.success(ItemResponse.from(item)));
    }

    /**
     * Returns the barcode as a PNG image.
     * Used by the React admin dashboard to preview barcodes.
     * Also usable by the Android app for display purposes.
     */
    @GetMapping(value = "/item/{itemId}/image", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get barcode as PNG image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long itemId) {
        byte[] png = barcodeService.generatePng(itemId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    /**
     * Returns a print-ready PDF label (57mm × 32mm).
     * Contains item name, barcode image, barcode value, and price.
     * Send this PDF to the Android POS printer via the device's print SDK.
     */
    @GetMapping(value = "/item/{itemId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Get barcode label as print-ready PDF")
    public ResponseEntity<byte[]> getPdfLabel(@PathVariable Long itemId) {
        byte[] pdf = barcodeService.generatePdfLabel(itemId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"label-item-" + itemId + ".pdf\"")
                .body(pdf);
    }
}
