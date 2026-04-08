-- ─────────────────────────────────────────────────────────────────────────────
-- V2: Flexible category hierarchy
--
-- Adds self-referential parent_id, level, and path to the categories table.
--
-- path example:  root → /1/    child → /1/5/    grandchild → /1/5/12/
-- This allows retrieving an entire subtree with a single:
--   WHERE path LIKE '/1/%'
--
-- level: 0 = root (Brand), 1 = Category, 2 = Sub-Category, etc.
--
-- Uniqueness rule changes: name is no longer globally unique.
--   A sibling uniqueness constraint is applied: UNIQUE(parent_id, name).
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Drop the old global unique name constraint
ALTER TABLE categories DROP INDEX uk_categories_name;

-- 2. Add hierarchy columns
ALTER TABLE categories
    ADD COLUMN parent_id BIGINT       NULL          AFTER description,
    ADD COLUMN level     INT          NOT NULL DEFAULT 0 AFTER parent_id,
    ADD COLUMN path      VARCHAR(500) NOT NULL DEFAULT '/' AFTER level;

-- 3. Initialise path + level for all existing root categories
UPDATE categories
SET path  = CONCAT('/', id, '/'),
    level = 0
WHERE parent_id IS NULL;

-- 4. Add foreign key (restrict delete — cannot delete a parent that has children)
ALTER TABLE categories
    ADD CONSTRAINT fk_categories_parent
    FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE RESTRICT;

-- 5. Sibling uniqueness: same name cannot appear twice under the same parent
--    NULL parent_id (roots) are treated as separate by MySQL UNIQUE NULL semantics
--    so two root-level categories can both be NULL but still distinct names.
--    We enforce root-name uniqueness separately via application logic.
ALTER TABLE categories
    ADD CONSTRAINT uk_categories_parent_name UNIQUE (parent_id, name);

-- 6. Supporting indexes
CREATE INDEX idx_categories_parent ON categories (parent_id);
CREATE INDEX idx_categories_path   ON categories (path(191));
CREATE INDEX idx_categories_level  ON categories (level);
