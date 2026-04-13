package com.pos.stock.entity;

import com.pos.item.entity.Item;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    private Item item;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "min_quantity", nullable = false)
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
     * Called from SaleService inside a transaction — let it propagate.
     */
    public void deduct(int amount) {
        if (this.quantity < amount) {
            throw new IllegalStateException(
                    "Insufficient stock for item id=" + item.getId()
                    + ". Available: " + this.quantity + ", Requested: " + amount);
        }
        this.quantity -= amount;
    }
}
