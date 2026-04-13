package com.pos.shopstock.entity;

import com.pos.item.entity.Item;
import com.pos.shop.entity.Shop;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shop_stock",
       uniqueConstraints = @UniqueConstraint(columnNames = {"shop_id", "item_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "min_quantity", nullable = false)
    @Builder.Default
    private Integer minQuantity = 5;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── helpers ──────────────────────────────────────────────────────────────

    public boolean isLow() {
        return quantity <= minQuantity;
    }

    public void add(int amount) {
        this.quantity += amount;
    }

    /**
     * Deducts stock. Throws IllegalStateException if insufficient.
     * Called from SaleService / TransferService inside a transaction.
     */
    public void deduct(int amount) {
        if (this.quantity < amount) {
            throw new IllegalStateException(
                    "Insufficient shop stock for item id=" + item.getId()
                    + " in shop id=" + shop.getId()
                    + ". Available: " + this.quantity + ", Requested: " + amount);
        }
        this.quantity -= amount;
    }
}
