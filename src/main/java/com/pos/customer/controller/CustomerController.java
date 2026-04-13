package com.pos.customer.controller;

import com.pos.common.response.ApiResponse;
import com.pos.customer.dto.CustomerRequest;
import com.pos.customer.dto.CustomerResponse;
import com.pos.customer.service.CustomerService;
import com.pos.invoice.dto.InvoiceResponse;
import com.pos.invoice.repository.InvoiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer management")
public class CustomerController {

    private final CustomerService    customerService;
    private final InvoiceRepository  invoiceRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'RECEPTION', 'CALL_CENTER')")
    @Operation(summary = "List customers with optional search")
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getAll(
            @RequestParam(required = false)    String search,
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size) {
        return ResponseEntity.ok(ApiResponse.success(
                customerService.getAll(search, page, size)));
    }

    @GetMapping("/phone/{phone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'RECEPTION', 'CALL_CENTER')")
    @Operation(summary = "Look up a customer by phone number — returns null body if not found")
    public ResponseEntity<ApiResponse<CustomerResponse>> findByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(ApiResponse.success(customerService.findByPhone(phone)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'RECEPTION', 'CALL_CENTER')")
    @Operation(summary = "Get customer by id")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                CustomerResponse.from(customerService.findOrThrow(id))));
    }

    @GetMapping("/{id}/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'RECEPTION', 'CALL_CENTER')")
    @Operation(summary = "Get all invoices for a customer, newest first")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getInvoices(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        customerService.findOrThrow(id);   // 404 if customer not found

        Page<InvoiceResponse> result = invoiceRepository
                .findByCustomerId(id, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(InvoiceResponse::from);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES', 'RECEPTION', 'CALL_CENTER')")
    @Operation(summary = "Create a new customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(customerService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES')")
    @Operation(summary = "Update customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(customerService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_BRANCHES')")
    @Operation(summary = "Delete customer")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
