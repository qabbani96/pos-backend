package com.pos.item.repository;

import com.pos.item.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    // ── Single-item lookups — fetch category + full parent chain ──────────────
    // This prevents LazyInitializationException in ItemResponse.buildCategoryPath()
    // which walks c.parent → c.parent.parent up the hierarchy.
    // Two separate LEFT JOIN FETCHes cover up to 3 hierarchy levels (brand/cat/sub-cat).

    @Query("""
            SELECT i FROM Item i
            LEFT JOIN FETCH i.category c
            LEFT JOIN FETCH c.parent p
            LEFT JOIN FETCH p.parent
            WHERE i.barcode = :barcode
            """)
    Optional<Item> findByBarcode(@Param("barcode") String barcode);

    @Query("""
            SELECT i FROM Item i
            LEFT JOIN FETCH i.category c
            LEFT JOIN FETCH c.parent p
            LEFT JOIN FETCH p.parent
            WHERE i.id = :id
            """)
    Optional<Item> findByIdWithCategory(@Param("id") Long id);

    Optional<Item> findBySkuAndActiveTrue(String sku);

    // ── Paginated list — active items only ────────────────────────────────────
    // JOIN FETCH on paginated queries requires a separate countQuery.
    // We fetch category + two parent levels to support buildCategoryPath.

    @Query(value = """
            SELECT i FROM Item i
            LEFT JOIN FETCH i.category c
            LEFT JOIN FETCH c.parent p
            LEFT JOIN FETCH p.parent
            WHERE i.active = true
              AND (:search IS NULL
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.sku)  LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR i.category.id = :categoryId)
            """,
            countQuery = """
            SELECT COUNT(i) FROM Item i
            WHERE i.active = true
              AND (:search IS NULL
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.sku)  LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR i.category.id = :categoryId)
            """)
    Page<Item> findAllActive(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    // ── Paginated list — all items (including inactive) ───────────────────────

    @Query(value = """
            SELECT i FROM Item i
            LEFT JOIN FETCH i.category c
            LEFT JOIN FETCH c.parent p
            LEFT JOIN FETCH p.parent
            WHERE (:search IS NULL
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.sku)  LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR i.category.id = :categoryId)
            """,
            countQuery = """
            SELECT COUNT(i) FROM Item i
            WHERE (:search IS NULL
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.sku)  LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR i.category.id = :categoryId)
            """)
    Page<Item> findAll(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            Pageable pageable);
}
