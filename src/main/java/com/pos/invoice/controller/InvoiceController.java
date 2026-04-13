package com.pos.invoice.controller;

import com.pos.common.response.ApiResponse;
import com.pos.invoice.dto.InvoiceRequest;
import com.pos.invoice.dto.InvoiceResponse;
import com.pos.invoice.dto.MoneySummary;
import com.pos.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Device repair invoice management")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'RECEPTION', 'CALL_CENTER')")
    @Operation(summary = "List invoices with optional filters")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getAll(
            @RequestParam(required = false)    Long   branchId,
            @RequestParam(required = false)    String status,
            @RequestParam(required = false)    String search,
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "15") int    size) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.getAll(branchId, status, search, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'RECEPTION', 'CALL_CENTER')")
    @Operation(summary = "Get invoice by id")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'RECEPTION', 'CALL_CENTER')")
    @Operation(summary = "Create a new invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(@RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'RECEPTION', 'CALL_CENTER')")
    @Operation(summary = "Update invoice fields")
    public ResponseEntity<ApiResponse<InvoiceResponse>> update(
            @PathVariable Long id, @RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES')")
    @Operation(summary = "Delete invoice (blocked if status is CHECKOUT)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/money")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES')")
    @Operation(summary = "Revenue summary — total amount and per-branch breakdown")
    public ResponseEntity<ApiResponse<MoneySummary>> money(
            @RequestParam(required = false) Long   branchId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.getMoneySummary(branchId, status)));
    }
}
