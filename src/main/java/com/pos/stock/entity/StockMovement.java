package com.pos.stock.entity;

import com.pos.auth.entity.User;
import com.pos.item.entity.Item;
import com.pos.shop.entity.Shop;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** Null = central warehouse movement; non-null = movement within a specific shop. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @Column(nullable = false)
    private Integer quantity;

    /** Stock balance before the movement (for audit). */
    @Column(name = "balance_before")
    private Integer balanceBefore;

    /** Stock balance after the movement (for audit). */
    @Column(name = "balance_after")
    private Integer balanceAfter;

    @Column(length = 100)
    private String reference;

    @Column(length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MovementType {
        /** Central stock added (receiving items into warehouse). */
        IN,
        /** Central stock removed (manual write-off). */
        OUT,
        /** Manual quantity correction on central or shop stock. */
        ADJUSTMENT,
        /** Central stock reduced — items sent to a shop. */
        TRANSFER_OUT,
        /** Shop stock increased — items received from central. */
        TRANSFER_IN,
        /** Shop stock reduced — items returned to central. */
        RETURN_OUT,
        /** Central stock increased — items returned from a shop. */
        RETURN_IN,
        /** Shop stock reduced by a completed sale. */
        SALE
    }
}
