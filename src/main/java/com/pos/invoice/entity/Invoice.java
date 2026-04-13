package com.pos.invoice.entity;

import com.pos.auth.entity.User;
import com.pos.branch.entity.Branch;
import com.pos.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", unique = true, length = 50)
    private String invoiceNumber;

    // ── Customer ─────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "customer_name",   length = 100)
    private String customerName;

    @Column(name = "customer_number", length = 30)
    private String customerNumber;

    // ── Branch ────────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    // ── Device ────────────────────────────────────────────────────────────────

    @Column(name = "device_type",        length = 100)
    private String deviceType;

    @Column(name = "device_color",       length = 50)
    private String deviceColor;

    @Column(name = "device_question",    columnDefinition = "TEXT")
    private String deviceQuestion;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_status",      length = 20)
    private DeviceStatus deviceStatus;

    @Column(name = "device_problem",     columnDefinition = "TEXT")
    private String deviceProblem;

    @Column(name = "device_imei",        length = 50)
    private String deviceImei;

    @Column(name = "device_note",        columnDefinition = "TEXT")
    private String deviceNote;

    @Column(name = "device_accessories", length = 255)
    private String deviceAccessories;

    @Column(name = "device_price",       precision = 10, scale = 2)
    private BigDecimal devicePrice;

    @Column(name = "hidden_price",       precision = 10, scale = 2)
    private BigDecimal hiddenPrice;

    // ── Call center ───────────────────────────────────────────────────────────

    @Column(name = "feedback_callcenter", columnDefinition = "TEXT")
    private String feedbackCallcenter;

    // ── Dates / times ─────────────────────────────────────────────────────────

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "entry_time")
    private LocalTime entryTime;

    @Column(name = "finish_main_date")
    private LocalDate finishMainDate;

    @Column(name = "finish_main_time")
    private LocalTime finishMainTime;

    @Column(name = "bill_date")
    private LocalDate billDate;

    @Column(name = "bill_time")
    private LocalTime billTime;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
