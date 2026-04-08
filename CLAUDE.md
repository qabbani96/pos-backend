# CLAUDE.md — pos-backend

This file provides guidance when working with the Spring Boot backend of the POS system.

## Commands

```bash
# Start MySQL (required before running the app)
docker-compose up -d

# Run the application
mvn spring-boot:run

# Build JAR
mvn clean package

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=SaleServiceTest

# Compile only (regenerates Lombok + MapStruct annotation processors)
mvn clean compile
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Architecture

Spring Boot 3.2.5 · Java 17 · MySQL 8 · Flyway · Spring Security + JWT · SpringDoc OpenAPI.

**Package layout** (`src/main/java/com/pos/`): each domain module contains its own `controller/`, `service/`, `entity/`, `repository/`, `dto/` sub-packages. Shared infrastructure lives in `common/`.

**Request flow:** `JwtAuthFilter` → Controller → Service (`@Transactional`) → Repository (Spring Data JPA / Hibernate) → MySQL 8.

### Module map

| Module | Responsibility |
|--------|----------------|
| `auth` | Login, JWT issuance/validation, in-memory logout blacklist |
| `item` | Product catalog CRUD, active/inactive toggle, paginated search with `categoryId` filter |
| `category` | Hierarchical category tree (Adjacency List + materialized path). Brand → Category → Sub-Category |
| `stock` | Inventory levels, stock movement history (IN / OUT / ADJUSTMENT), atomic deduction on sale |
| `barcode` | ZXing (CODE128 / EAN-13 / QR → PNG) + PDFBox (57×32mm thermal label PDF) |
| `sale` | Atomic POS checkout: validate stock → create Sale + SaleItem snapshots → deduct stock → record movements |
| `report` | Dashboard summary, daily revenue, top items, cashier performance — all via native SQL |
| `common` | `ApiResponse<T>` wrapper, `GlobalExceptionHandler`, `SecurityConfig` (CORS, RBAC) |

---

## Category Hierarchy

Categories use an **Adjacency List** with a **materialized path**:

- `parentId` — nullable FK to parent category (`null` = root / Brand)
- `level` — 0 = Brand, 1 = Category, 2 = Sub-Category
- `path` — string like `/1/5/12/` for DFS without recursion

**Key endpoints:**
- `GET /api/v1/categories` — flat list (all categories, sorted by level then name)
- `GET /api/v1/categories/tree` — nested tree with `children[]` populated
- `GET /api/v1/categories/roots` — only level-0 brands
- `GET /api/v1/categories/{id}/children` — direct children of a node

**`CategoryResponse`** includes: `id`, `name`, `description`, `parentId`, `parentName`, `level`, `path`, `children[]`.

**`ItemResponse`** includes `categoryPath` — a human-readable breadcrumb string built server-side (e.g. `"Apple › Phones › iPhone 15"`). This is used by the Android app to display the category in the cart and last-scanned bar.

---

## Database

- **MySQL 8.0** via Docker Compose (`localhost:3306`, db: `pos_db`, user: `pos_user`, pass: `pos_pass`)
- **Flyway** manages all schema — add new `V{n}__description.sql` files under `src/main/resources/db/migration/`. Never modify existing ones. `ddl-auto: validate` means Hibernate won't auto-alter the schema.
- `sale_items` stores `item_name` and `unit_price` **snapshots** at transaction time — not FK references to current values.
- Soft deletes via `active` boolean on `users` and `items`.

---

## Security

- **Roles:** `ADMIN` (full access) and `CASHIER` (read catalog, create sales, view own sales). Enforced with `@PreAuthorize` on controllers.
- Passwords are BCrypt-hashed.
- JWT secret is Base64-encoded in `application.yml` — **rotate before deploying to production**.
- CORS currently allows all origins (`*`) — tighten for production.

---

## API Conventions

- All endpoints prefixed `/api/v1/`.
- All responses wrapped: `{ success, message, data, errorCode }`.
- Paginated endpoints accept `page`, `size`, `sort` (standard Spring `Pageable`).
- `BusinessException` and `ResourceNotFoundException` are caught by `GlobalExceptionHandler` (`@RestControllerAdvice`).

---

## Stock Movement Types

| Type | Behaviour |
|------|-----------|
| `IN` | Adds to existing quantity (e.g. receiving a shipment) |
| `OUT` | Subtracts from existing quantity (e.g. damaged goods) |
| `ADJUSTMENT` | Overrides to an exact quantity (e.g. physical stocktake reconciliation) |

Sale deductions are recorded as an internal `SALE` movement type automatically.

---

## MapStruct + Lombok

Both use annotation processors. The `maven-compiler-plugin` in `pom.xml` runs Lombok before MapStruct. If you see compilation errors after adding new mappers or Lombok-annotated classes, run `mvn clean compile` to regenerate.

---

## Barcode Files

Generated barcode PNGs are stored in `./uploads/barcodes/` (relative to working directory at startup). This directory is created at runtime. The label PDF is streamed directly as a download — not stored on disk.

---

## Known Gotchas

- **JPQL FETCH JOIN + pagination:** Any repository method that joins a collection with `JOIN FETCH` must include an explicit `countQuery` on the `@Query` annotation, otherwise Spring Data will generate a broken count query and throw at runtime.
- **Native SQL aggregates:** `SUM()` can return `Integer` or `Long` instead of `BigDecimal` when the result set is empty. Use `COALESCE(SUM(x), 0)` with care — prefer returning `null` and handling it in the service layer.
- **`categoryPath` is computed at read time** — it's not stored in the DB. If the category tree is very deep or has many items, consider caching it.
