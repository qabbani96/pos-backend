package com.pos.item.dto;

import com.pos.item.entity.Category;

import java.util.Collections;
import java.util.List;

/**
 * Dual-use DTO:
 *  - Flat  (children = null)  → used in dropdown lists, item forms
 *  - Tree  (children populated) → used by GET /categories/tree
 */
public record CategoryResponse(
        Long   id,
        String name,
        String description,
        Long   parentId,
        String parentName,
        int    level,
        String path,

        /**
         * Direct children — null in flat responses, populated in tree responses.
         */
        List<CategoryResponse> children
) {

    // ── Factory: flat (no children) ───────────────────────────────────────────

    public static CategoryResponse flat(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getParentId(),
                c.getParentName(),
                c.getLevel(),
                c.getPath(),
                null
        );
    }

    // ── Factory: tree node (with empty children list, to be populated) ────────

    public static CategoryResponse treeNode(Category c, List<CategoryResponse> children) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getParentId(),
                c.getParentName(),
                c.getLevel(),
                c.getPath(),
                children
        );
    }

    // ── Convenience: leaf node in tree (no children) ──────────────────────────

    public static CategoryResponse leaf(Category c) {
        return treeNode(c, Collections.emptyList());
    }
}
