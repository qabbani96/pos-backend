package com.pos.item.service;

import com.pos.common.exception.BusinessException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.item.dto.CategoryRequest;
import com.pos.item.dto.CategoryResponse;
import com.pos.item.entity.Category;
import com.pos.item.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ── Read — flat list ──────────────────────────────────────────────────────

    /** Returns every category as a flat list sorted by level then name.
     *  Used for dropdowns in item forms. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllWithParent()
                .stream()
                .map(CategoryResponse::flat)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryResponse.flat(getOrThrow(id));
    }

    // ── Read — tree ───────────────────────────────────────────────────────────

    /**
     * Returns the full category hierarchy as a forest of root nodes.
     * Each node has its children list populated recursively.
     * Does a single DB query then assembles the tree in memory — safe for
     * POS-scale category counts (hundreds, not millions).
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findTree() {
        List<Category> all = categoryRepository.findAllWithParent();
        return buildTree(all, null);
    }

    /**
     * Returns the direct children of a given parent (one level only).
     * Pass null to get root categories.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findChildren(Long parentId) {
        List<Category> children = parentId == null
                ? categoryRepository.findByParentIsNullOrderByNameAsc()
                : categoryRepository.findByParent_IdOrderByNameAsc(parentId);
        return children.stream().map(CategoryResponse::flat).toList();
    }

    /**
     * Returns the entire subtree rooted at the given category (inclusive).
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findSubtree(Long rootId) {
        Category root = getOrThrow(rootId);
        return categoryRepository.findSubtree(root.getPath())
                .stream()
                .map(CategoryResponse::flat)
                .toList();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category parent = resolveParent(request.parentId());

        // Sibling uniqueness check
        validateSiblingName(request.name(), request.parentId(), null);

        Category category = Category.builder()
                .name(request.name().trim())
                .description(request.description())
                .parent(parent)
                .level(parent != null ? parent.getLevel() + 1 : 0)
                .path("/")                // temporary — updated after insert
                .build();

        Category saved = categoryRepository.save(category);

        // Set final path now that we have the generated ID
        saved.setPath(buildPath(parent, saved.getId()));
        categoryRepository.save(saved);

        log.info("action=category_created, id={}, name={}, level={}, parentId={}",
                saved.getId(), saved.getName(), saved.getLevel(), request.parentId());

        return CategoryResponse.flat(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getOrThrow(id);
        Long newParentId = request.parentId();

        // Prevent circular hierarchy (can't set own descendant as parent)
        if (newParentId != null && isDescendant(category, newParentId)) {
            throw new BusinessException("CIRCULAR_HIERARCHY",
                    "Cannot set a descendant as the parent");
        }

        // Sibling uniqueness — exclude current node
        validateSiblingName(request.name(), newParentId, id);

        boolean reparenting = !java.util.Objects.equals(
                category.getParentId(), newParentId);

        category.setName(request.name().trim());
        category.setDescription(request.description());

        if (reparenting) {
            Category newParent = resolveParent(newParentId);
            String oldPath     = category.getPath();
            int    oldLevel    = category.getLevel();
            int    newLevel    = newParent != null ? newParent.getLevel() + 1 : 0;

            category.setParent(newParent);
            category.setLevel(newLevel);
            category.setPath(buildPath(newParent, id));

            // Cascade path + level changes to all descendants
            rebuildDescendantPaths(oldPath, oldLevel, category);
        }

        log.info("action=category_updated, id={}, reparented={}", id, reparenting);
        return CategoryResponse.flat(category);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        Category category = getOrThrow(id);

        if (categoryRepository.existsByParent_Id(id)) {
            throw new BusinessException("HAS_CHILDREN",
                    "Cannot delete category that has sub-categories. " +
                    "Delete or reassign children first.");
        }
        if (categoryRepository.hasItems(id)) {
            throw new BusinessException("HAS_ITEMS",
                    "Cannot delete category that has items assigned to it. " +
                    "Reassign items first.");
        }

        categoryRepository.delete(category);
        log.info("action=category_deleted, id={}, name={}", id, category.getName());
    }

    // ── Tree builder ──────────────────────────────────────────────────────────

    /**
     * Builds a recursive tree from a flat list of categories.
     * Single pass: O(n). Groups children by parentId using a HashMap.
     */
    private List<CategoryResponse> buildTree(List<Category> all, Long parentId) {
        // Group by parentId
        Map<Long, List<Category>> byParent = new HashMap<>();
        for (Category c : all) {
            Long key = c.getParentId();           // null for roots
            byParent.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }
        return collectChildren(byParent, parentId);
    }

    private List<CategoryResponse> collectChildren(
            Map<Long, List<Category>> byParent, Long parentId) {

        List<Category> nodes = byParent.getOrDefault(parentId, List.of());
        List<CategoryResponse> result = new ArrayList<>();
        for (Category c : nodes) {
            List<CategoryResponse> children = collectChildren(byParent, c.getId());
            result.add(CategoryResponse.treeNode(c, children));
        }
        return result;
    }

    // ── Cascade path rebuild ──────────────────────────────────────────────────

    /**
     * After reparenting a category, update path and level for all descendants.
     * Loads the subtree by old path prefix, then updates each node.
     */
    private void rebuildDescendantPaths(String oldPathPrefix,
                                        int    oldLevel,
                                        Category movedCategory) {

        // findSubtree adds the '%' wildcard internally via CONCAT — pass prefix only
        List<Category> descendants =
                categoryRepository.findSubtree(oldPathPrefix);
        // filter out the moved node itself
        descendants = descendants.stream()
                .filter(d -> !d.getId().equals(movedCategory.getId()))
                .collect(Collectors.toList());

        String newPathPrefix = movedCategory.getPath();
        int    levelDelta    = movedCategory.getLevel() - oldLevel;

        for (Category d : descendants) {
            // Replace the old prefix with the new one
            String newPath = newPathPrefix + d.getPath().substring(oldPathPrefix.length());
            d.setPath(newPath);
            d.setLevel(d.getLevel() + levelDelta);
        }
        // saveAll in one flush
        categoryRepository.saveAll(descendants);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Category resolveParent(Long parentId) {
        if (parentId == null) return null;
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parent category not found: " + parentId));
    }

    private void validateSiblingName(String name, Long parentId, Long excludeId) {
        boolean conflict = excludeId == null
                ? categoryRepository.existsByNameAndParent(name, parentId)
                : categoryRepository.existsByNameAndParentExcluding(name, parentId, excludeId);
        if (conflict) {
            throw new BusinessException("DUPLICATE_CATEGORY",
                    "A category named '" + name + "' already exists under this parent.");
        }
    }

    private String buildPath(Category parent, Long nodeId) {
        String parentPath = parent != null ? parent.getPath() : "/";
        return parentPath + nodeId + "/";
    }

    /**
     * Returns true if candidateAncestorId is actually a descendant of category.
     * Used to prevent circular hierarchy during reparenting.
     */
    private boolean isDescendant(Category category, Long candidateParentId) {
        // If candidateParentId is within category's subtree — circular
        List<Category> subtree = categoryRepository.findSubtree(category.getPath());
        return subtree.stream().anyMatch(c -> c.getId().equals(candidateParentId));
    }

    public Category getOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + id));
    }
}
