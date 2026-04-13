package com.pos.item.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    // ── Hierarchy ─────────────────────────────────────────────────────────────

    /**
     * Direct parent. Null means this is a root category (e.g. a Brand).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /**
     * Direct children — populated on demand. Never serialize directly;
     * use the CategoryService tree builder instead.
     */
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    /**
     * Depth in the tree. 0 = root (Brand), 1 = Category, 2 = Sub-Category, …
     * Stored to avoid recursive depth queries.
     */
    @Column(nullable = false)
    private int level;

    /**
     * Materialized path — e.g. "/1/5/12/".
     * Enables single-query subtree retrieval:  WHERE path LIKE '/1/%'
     * Set by CategoryService after insert/reparent.
     */
    @Column(nullable = false, length = 500)
    @Builder.Default
    private String path = "/";

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isRoot() {
        return parent == null;
    }

    public Long getParentId() {
        return parent != null ? parent.getId() : null;
    }

    public String getParentName() {
        return parent != null ? parent.getName() : null;
    }
}
