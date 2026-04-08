package com.pos.item.dto;

import com.pos.item.entity.Category;
import com.pos.item.entity.Item;
import com.pos.item.entity.Item.BarcodeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ItemResponse(
        Long        id,
        String      sku,
        String      name,
        String      description,
        Long        categoryId,
        String      categoryName,

        /**
         * Human-readable full path to the category.
         * Example: "Apple › Screens › iPhone 15 Pro"
         * Null if the item has no category assigned.
         */
        String      categoryPath,

        BigDecimal  price,
        BigDecimal  costPrice,
        String      barcode,
        BarcodeType barcodeType,
        String      imageUrl,
        Boolean     active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ItemResponse from(Item item) {
        Category cat = item.getCategory();
        return new ItemResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getDescription(),
                cat != null ? cat.getId()   : null,
                cat != null ? cat.getName() : null,
                cat != null ? buildCategoryPath(cat) : null,
                item.getPrice(),
                item.getCostPrice(),
                item.getBarcode(),
                item.getBarcodeType(),
                item.getImageUrl(),
                item.getActive(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    /**
     * Walks up the parent chain from the assigned category to the root
     * and returns a formatted breadcrumb string.
     *
     * "Apple › Screens › iPhone 15 Pro"
     *
     * Note: relies on Hibernate lazy-loading the parent chain.
     * Works correctly when called inside a @Transactional context.
     */
    private static String buildCategoryPath(Category category) {
        List<String> parts = new ArrayList<>();
        Category current = category;
        while (current != null) {
            parts.add(current.getName());
            current = current.getParent();
        }
        Collections.reverse(parts);
        return String.join(" › ", parts);
    }
}
