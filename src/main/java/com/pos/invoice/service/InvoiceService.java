package com.pos.invoice.service;

import com.pos.auth.entity.User;
import com.pos.auth.repository.UserRepository;
import com.pos.branch.entity.Branch;
import com.pos.branch.repository.BranchRepository;
import com.pos.common.exception.BusinessException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.customer.entity.Customer;
import com.pos.customer.repository.CustomerRepository;
import com.pos.invoice.dto.InvoiceRequest;
import com.pos.invoice.dto.InvoiceResponse;
import com.pos.invoice.dto.MoneySummary;
import com.pos.invoice.entity.DeviceStatus;
import com.pos.invoice.entity.Invoice;
import com.pos.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository  invoiceRepository;
    private final BranchRepository   branchRepository;
    private final UserRepository     userRepository;
    private final CustomerRepository customerRepository;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAll(Long branchId, String statusStr, String search,
                                        int page, int size) {
        DeviceStatus status   = parseStatus(statusStr);
        PageRequest  pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return invoiceRepository.search(branchId, status, nullIfBlank(search), pageable)
                .map(InvoiceResponse::from);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        return InvoiceResponse.from(findOrThrow(id));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        Branch   branch   = resolveCreatorBranch(request.branchId());
        Customer customer = resolveCustomer(request.customerName(), request.customerNumber());

        Invoice invoice = Invoice.builder()
                .branch(branch)
                .customer(customer)
                .customerName(request.customerName())
                .customerNumber(request.customerNumber())
                .deviceType(request.deviceType())
                .deviceColor(request.deviceColor())
                .deviceQuestion(request.deviceQuestion())
                .deviceStatus(DeviceStatus.PENDING)     // always starts PENDING
                .deviceProblem(request.deviceProblem())
                .deviceImei(request.deviceImei())
                .deviceNote(request.deviceNote())
                .deviceAccessories(request.deviceAccessories())
                .devicePrice(request.devicePrice())
                .hiddenPrice(request.hiddenPrice())
                .feedbackCallcenter(request.feedbackCallcenter())
                .entryDate(parseDate(request.entryDate()))
                .entryTime(parseTime(request.entryTime()))
                .createdBy(currentUser())
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        // Generate invoice number using the auto-generated PK
        saved.setInvoiceNumber(buildInvoiceNumber(saved.getId(), saved.getCreatedAt()));
        invoiceRepository.save(saved);

        log.info("action=invoice_created, id={}, number={}", saved.getId(), saved.getInvoiceNumber());
        return InvoiceResponse.from(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse update(Long id, InvoiceRequest request) {
        Invoice invoice = findOrThrow(id);

        if (request.customerName()       != null) invoice.setCustomerName(request.customerName());
        if (request.customerNumber()     != null) invoice.setCustomerNumber(request.customerNumber());
        if (request.deviceType()         != null) invoice.setDeviceType(request.deviceType());
        if (request.deviceColor()        != null) invoice.setDeviceColor(request.deviceColor());
        if (request.deviceQuestion()     != null) invoice.setDeviceQuestion(request.deviceQuestion());
        if (request.deviceProblem()      != null) invoice.setDeviceProblem(request.deviceProblem());
        if (request.deviceImei()         != null) invoice.setDeviceImei(request.deviceImei());
        if (request.deviceNote()         != null) invoice.setDeviceNote(request.deviceNote());
        if (request.deviceAccessories()  != null) invoice.setDeviceAccessories(request.deviceAccessories());
        if (request.devicePrice()        != null) invoice.setDevicePrice(request.devicePrice());
        if (request.hiddenPrice()        != null) invoice.setHiddenPrice(request.hiddenPrice());
        if (request.feedbackCallcenter() != null) invoice.setFeedbackCallcenter(request.feedbackCallcenter());
        if (request.deviceStatus()       != null) invoice.setDeviceStatus(parseStatusStrict(request.deviceStatus()));
        if (request.entryDate()          != null) invoice.setEntryDate(parseDate(request.entryDate()));
        if (request.entryTime()          != null) invoice.setEntryTime(parseTime(request.entryTime()));
        if (request.finishMainDate()     != null) invoice.setFinishMainDate(parseDate(request.finishMainDate()));
        if (request.finishMainTime()     != null) invoice.setFinishMainTime(parseTime(request.finishMainTime()));
        if (request.billDate()           != null) invoice.setBillDate(parseDate(request.billDate()));
        if (request.billTime()           != null) invoice.setBillTime(parseTime(request.billTime()));

        log.info("action=invoice_updated, id={}", id);
        return InvoiceResponse.from(invoice);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        Invoice invoice = findOrThrow(id);
        if (invoice.getDeviceStatus() == DeviceStatus.CHECKOUT) {
            throw new BusinessException("INVOICE_CHECKED_OUT",
                    "Cannot delete a checked-out invoice. Change its status first.");
        }
        invoiceRepository.delete(invoice);
        log.info("action=invoice_deleted, id={}", id);
    }

    // ── Money summary ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MoneySummary getMoneySummary(Long branchId, String statusStr) {
        DeviceStatus status = parseStatus(statusStr);

        Object[] totals      = invoiceRepository.sumAndCount(branchId, status);
        BigDecimal total     = (BigDecimal) totals[0];
        long       count     = ((Number)    totals[1]).longValue();

        List<MoneySummary.BranchLine> breakdown = List.of();
        if (branchId == null) {
            breakdown = invoiceRepository.breakdownByBranch(status).stream()
                    .map(row -> new MoneySummary.BranchLine(
                            ((Number)    row[0]).longValue(),
                            (String)     row[1],
                            (BigDecimal) row[2],
                            ((Number)    row[3]).longValue()
                    ))
                    .toList();
        }

        return new MoneySummary(total, count, breakdown);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Invoice findOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    /** Resolves branch: prefer the explicit requestedBranchId, fall back to the caller's own branch. */
    private Branch resolveCreatorBranch(Long requestedBranchId) {
        if (requestedBranchId != null) {
            return branchRepository.findById(requestedBranchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + requestedBranchId));
        }
        User user = currentUser();
        return user != null ? user.getBranch() : null;
    }

    /** Upserts a customer by phone number. Creates one if not found. */
    private Customer resolveCustomer(String name, String phone) {
        if (phone == null || phone.isBlank()) return null;
        return customerRepository.findByCustomerNumber(phone.trim())
                .orElseGet(() -> {
                    Customer c = Customer.builder()
                            .customerName(name != null ? name.trim() : "Unknown")
                            .customerNumber(phone.trim())
                            .build();
                    return customerRepository.save(c);
                });
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private String buildInvoiceNumber(Long id, LocalDateTime createdAt) {
        return "INV-" + createdAt.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + id;
    }

    private DeviceStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        return parseStatusStrict(status);
    }

    private DeviceStatus parseStatusStrict(String status) {
        try {
            return DeviceStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS", "Unknown device status: " + status);
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalTime.parse(value);
    }

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
