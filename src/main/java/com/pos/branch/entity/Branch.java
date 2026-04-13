package com.pos.branch.entity;

import com.pos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a physical branch location.
 * Users with RECEPTION or CALL_CENTER roles belong to a branch.
 * Shops (POS locations) are optionally linked to a branch.
 */
@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_name", nullable = false, unique = true, length = 100)
    private String branchName;

    /** Contact number printed on receipts. Nullable. */
    @Column(name = "mobile", length = 20)
    private String mobile;

    /**
     * Thermal paper width in mm. Typical values: 58 or 80. Default 58.
     * columnDefinition = "tinyint unsigned" matches the DB column type added in V8.
     * Without this, Hibernate validate mode rejects the TINYINT vs INTEGER mismatch.
     */
    @Column(name = "receipt_width_mm", nullable = false, columnDefinition = "tinyint unsigned")
    @Builder.Default
    private int receiptWidthMm = 58;

    /**
     * Thermal paper height in mm. Null = auto (content-driven height).
     * columnDefinition = "tinyint unsigned" matches the DB column type (added in V9).
     */
    @Column(name = "receipt_height_mm", columnDefinition = "tinyint unsigned")
    private Integer receiptHeightMm;
}
