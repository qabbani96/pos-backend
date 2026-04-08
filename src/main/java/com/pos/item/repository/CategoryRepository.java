package com.pos.item.repository;

import com.pos.item.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ── Hierarchy queries ─────────────────────────────────────────────────────

    /** All root categories (level 0 — Brands) ordered by name. */
    List<Category> findByParentIsNullOrderByNameAsc();

    /**
     * Direct children of a given parent.
     * Use parent_Id (underscore) — Spring Data traverses parent.id,
     * not a direct field named parentId.
     */
    List<Category> findByParent_IdOrderByNameAsc(Long parentId);

    /**
     * Entire subtree rooted at the given path prefix.
     * Caller passes the path without a trailing wildcard, e.g. "/1/5/".
     * CONCAT adds the '%' so the LIKE becomes: path LIKE '/1/5/%'
     */
    @Query("SELECT c FROM Category c " +
           "WHERE c.path LIKE CONCAT(:pathPrefix, '%') " +
           "ORDER BY c.level ASC, c.name ASC")
    List<Category> findSubtree(@Param("pathPrefix") String pathPrefix);

    /** All nodes at a specific depth. */
    List<Category> findByLevelOrderByNameAsc(int level);

    /** All categories, ordered for efficient in-memory tree assembly. */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent ORDER BY c.level ASC, c.name ASC")
    List<Category> findAllWithParent();

    // ── Uniqueness checks ─────────────────────────────────────────────────────

    /**
     * Sibling uniqueness: same name cannot exist twice under the same parent.
     * NULL parentId means root level.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM Category c
            WHERE LOWER(c.name) = LOWER(:name)
              AND ((:parentId IS NULL AND c.parent IS NULL)
                   OR c.parent.id = :parentId)
            """)
    boolean existsByNameAndParent(@Param("name")     String name,
                                  @Param("parentId") Long   parentId);

    /** Same check, excluding a specific node (used during update). */
    @Query("""
            SELECT COUNT(c) > 0 FROM Category c
            WHERE LOWER(c.name) = LOWER(:name)
              AND c.id <> :excludeId
              AND ((:parentId IS NULL AND c.parent IS NULL)
                   OR c.parent.id = :parentId)
            """)
    boolean existsByNameAndParentExcluding(@Param("name")      String name,
                                           @Param("parentId")  Long   parentId,
                                           @Param("excludeId") Long   excludeId);

    // ── Safety checks before delete ───────────────────────────────────────────

    /**
     * True if this category has at least one direct child.
     * Uses parent_Id (underscore) for nested property traversal.
     */
    boolean existsByParent_Id(Long parentId);

    /** True if any item is assigned to this category. */
    @Query("SELECT COUNT(i) > 0 FROM Item i WHERE i.category.id = :categoryId")
    boolean hasItems(@Param("categoryId") Long categoryId);

    // ── Legacy compat ─────────────────────────────────────────────────────────

    Optional<Category> findByNameIgnoreCase(String name);
}
