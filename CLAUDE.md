# CLAUDE.md — pos-backend

This file provides guidance when working with the Spring Boot backend of the POS system.

## Commands

```bash
# Start the application (dev)
mvn spring-boot:run

# Build without running tests
mvn clean package -DskipTests

# Run tests
mvn test

# Start MySQL via Docker Compose
docker-compose up -d

# Stop MySQL
docker-compose down
```

**Swagger UI:** http://localhost:8080/swagger-ui.html
**API docs JSON:** http://localhost:8080/v3/api-docs

---

## Tech Stack

- **Java 17** + **Spring Boot 3.2.5**
- **MySQL 8** — primary database
- **Spring Data JPA / Hibernate** — ORM (ddl-auto: validate — Flyway owns the schema)
- **Flyway** — DB migrations in `src/main/resources/db/migration/`
- **Spring Security + JWT** — stateless auth (Bearer token, 24h expiry)
- **ZXing** — barcode image generation (PNG, Code 128)
- **SpringDoc / Swagger** — API documentation at `/swagger-ui.html`
- **HikariCP** — connection pool (max 10)

---

## Project Structure

```
src/main/java/com/pos/
├── PosApplication.java
├── auth/
│   ├── controller/AuthController.java       # POST /api/v1/auth/login
│   ├── dto/                                 # LoginRequest, LoginResponse
│   ├── entity/                              # User, Role (enum: ADMIN, CASHIER)
│   ├── repository/UserRepository.java
│   ├── security/
│   │   ├── JwtAuthFilter.java              # OncePerRequestFilter — validates Bearer token
│   │   ├── JwtService.java                 # generateToken(), validateToken(), extractUsername()
│   │   └── UserDetailsServiceImpl.java
│   └── service/AuthService.java
├── barcode/
│   ├── controller/BarcodeController.java   # GET /api/v1/barcodes/{itemId}/image
│   └── service/BarcodeService.java         # Generates PNG; stored under ./uploads/barcodes/
├── common/
│   ├── config/
│   │   ├── DataInitializer.java            # Seeds default admin user on first startup
│   │   ├── OpenApiConfig.java              # Swagger JWT bearer config
│   │   └── SecurityConfig.java            # CORS + JWT filter chain
│   ├── entity/BaseEntity.java              # @MappedSuperclass with createdAt, updatedAt
│   ├── exception/
│   │   ├── BusinessException.java          # -> 400 Bad Request
│   │   ├── ResourceNotFoundException.java  # -> 404 Not Found
│   │   └── GlobalExceptionHandler.java     # @RestControllerAdvice
│   └── response/ApiResponse.java           # { success, message, data }
├── item/
│   ├── controller/
│   │   ├── ItemController.java             # CRUD + activate/deactivate
│   │   └── CategoryController.java         # CRUD + tree endpoint
│   ├── dto/                                # ItemRequest, ItemResponse, CategoryRequest, CategoryResponse
│   ├── entity/
│   │   ├── Item.java                       # name, sku, barcode, price, active, category FK
│   │   └── Category.java                   # name, parentId (nullable), level, path
│   ├── repository/
│   │   ├── ItemRepository.java             # findByBarcodeValue(), paginated filter query
│   │   └── CategoryRepository.java         # findByParentIsNull(), findByParentId()
│   └── service/
│       ├── ItemService.java
│       └── CategoryService.java            # buildPath(), buildTree()
├── report/
│   ├── controller/ReportController.java    # GET /api/v1/reports/*
│   ├── dto/                                # DashboardSummary, DailySalesReport, TopItemReport, CashierReport
│   ├── repository/ReportRepository.java    # Native SQL aggregation queries
│   └── service/ReportService.java
├── sale/
│   ├── controller/SaleController.java      # POST /api/v1/sales, GET with filters
│   ├── dto/                                # SaleRequest, SaleResponse, SaleItemRequest, SaleItemResponse
│   ├── entity/
│   │   ├── Sale.java                       # total, status (COMPLETED/REFUNDED), cashier FK, createdAt
│   │   └── SaleItem.java                   # sale FK, item FK, quantity, unitPrice
│   ├── repository/
│   │   ├── SaleRepository.java
│   │   └── SaleItemRepository.java
│   └── service/SaleService.java            # createSale() deducts stock atomically in one @Transactional
└── stock/
    ├── controller/StockController.java     # GET, PATCH /adjust, GET /movements
    ├── dto/                                # StockResponse, StockAdjustRequest, StockMovementResponse, StockSummary
    ├── entity/
    │   ├── Stock.java                      # item FK (1-to-1), quantity, lowStockThreshold
    │   └── StockMovement.java              # stock FK, type, quantityChange, note, createdAt
    ├── repository/
    │   ├── StockRepository.java
    │   └── StockMovementRepository.java
    └── service/StockService.java           # adjust() records movement + updates quantity atomically

src/main/resources/
├── application.yml                         # All runtime config
└── db/migration/
    ├── V1__init_schema.sql                 # Full initial schema
    └── V2__category_hierarchy.sql          # Adds level + path columns to categories
```

---

## API Endpoints

### Auth
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/login` | Public | Returns JWT token |

### Items
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/items` | Yes | List (search, categoryId, activeOnly, page, size) |
| POST | `/api/v1/items` | Yes | Create item |
| GET | `/api/v1/items/{id}` | Yes | Get by ID |
| PUT | `/api/v1/items/{id}` | Yes | Update item |
| PATCH | `/api/v1/items/{id}/activate` | Yes | Set active = true |
| PATCH | `/api/v1/items/{id}/deactivate` | Yes | Set active = false |
| DELETE | `/api/v1/items/{id}` | Yes | Delete item |
| GET | `/api/v1/items/barcode/{value}` | Yes | Lookup by barcode (used by Android POS) |

### Categories
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/categories` | Yes | Flat list (optional parentId filter) |
| GET | `/api/v1/categories/tree` | Yes | Full nested tree |
| GET | `/api/v1/categories/roots` | Yes | Brand-level nodes only |
| GET | `/api/v1/categories/{id}/children` | Yes | Direct children |
| POST | `/api/v1/categories` | Yes | Create |
| PUT | `/api/v1/categories/{id}` | Yes | Update |
| DELETE | `/api/v1/categories/{id}` | Yes | Delete (only if no children or items attached) |

### Stock
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/stock` | Yes | All stock (lowOnly flag) |
| GET | `/api/v1/stock/{itemId}` | Yes | Single item stock |
| PATCH | `/api/v1/stock/{itemId}/adjust` | Yes | Adjust quantity (positive or negative) |
| GET | `/api/v1/stock/{itemId}/movements` | Yes | Movement history |

### Sales
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/sales` | Yes | Create sale — auto-deducts stock |
| GET | `/api/v1/sales` | Yes | List (from, to, status, cashier, page, size) |
| GET | `/api/v1/sales/{id}` | Yes | Sale details with line items |
| PATCH | `/api/v1/sales/{id}/refund` | Yes | Mark sale as REFUNDED |

### Reports
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/reports/dashboard` | Yes | KPI summary for today |
| GET | `/api/v1/reports/daily` | Yes | Daily totals (from, to) |
| GET | `/api/v1/reports/top-items` | Yes | Top selling items (from, to, limit) |
| GET | `/api/v1/reports/cashier` | Yes | Sales per cashier (from, to) |

### Barcodes
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/barcodes/{itemId}/image` | Yes | Download barcode PNG |

---

## Key Patterns

### Unified API response
Every endpoint returns the same envelope — never return raw objects from controllers:
```java
return ResponseEntity.ok(ApiResponse.success(data, "Items fetched"));

// Errors — throw, the GlobalExceptionHandler maps them:
throw new ResourceNotFoundException("Item not found: " + id);  // -> 404
throw new BusinessException("Insufficient stock for: " + sku); // -> 400
```

### Layer responsibilities
- **Controller** — `@Valid` input validation, delegate to service, return `ApiResponse`
- **Service** — all business logic, `@Transactional` boundaries
- **Repository** — `JpaRepository` only, no business logic

### Pagination
All list endpoints accept `page` (0-based) and `size` (default 20):
```java
Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
Page<Item> result = itemRepository.findAllWithFilters(search, categoryId, activeOnly, pageable);
```

---

## Category Hierarchy

3-level tree: **Brand (level 0) -> Category (level 1) -> Sub-Category (level 2)**

Each node stores a **materialized path** (`path` column) for efficient tree queries:
- Brand: `"1"`
- Category: `"1/5"`
- Sub-Category: `"1/5/12"`

`CategoryService.buildPath()` computes this on every create/update.

`CategoryResponse` includes a `categoryPath` breadcrumb string (`"Brand > Category > Sub"`) consumed by the Android app and the dashboard.

---

## Stock Movement Types

| Type | Triggered by |
|------|-------------|
| `PURCHASE` | Manual stock increase (restocking) |
| `SALE` | Auto-deducted by `SaleService.createSale()` |
| `ADJUSTMENT` | Manual admin correction (positive or negative) |
| `REFUND` | Stock restored when a sale is marked REFUNDED |

---

## Database

### Connection (dev defaults in application.yml)
```
Host:     localhost:3306
Database: pos_db
User:     pos_user
Password: pos_pass
```

### Flyway migrations
- `V1__init_schema.sql` — full initial schema (all tables)
- `V2__category_hierarchy.sql` — adds `level` and `path` columns to categories

Never edit existing migration files. Always add a new `V{n}__description.sql`.

### Default admin account (seeded by DataInitializer)
```
username: admin
password: admin123
```
Change this before going to production.

---

## Security

- All endpoints except `POST /api/v1/auth/login` require `Authorization: Bearer <token>`
- JWT secret is in `application.yml` under `jwt.secret` — **change before production**
- CORS is configured in `SecurityConfig.java`
- Allowed methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`
- **`PATCH` must stay in allowed methods** — `/activate`, `/deactivate`, `/adjust`, and
  `/refund` all use `@PatchMapping`. Removing `PATCH` causes preflight CORS rejection.

---

## Known Gotchas

- **`ddl-auto: validate`** — Hibernate does NOT create or alter tables. Add a Flyway
  migration before adding any entity field, or the app fails on startup with a
  schema-validation error.
- **`categoryPath` on `ItemResponse`** — built as `"Brand > Category > Sub"`. The Android
  app displays this directly; do not change the separator without updating the app.
- **`DataInitializer`** checks if admin exists before inserting — safe on every startup.
- **`baseline-on-migrate: true`** — required if Flyway runs against a DB that already has
  tables but no `flyway_schema_history` row.
- **JWT expiry is 24 hours** — the Android app does not auto-refresh; users must re-login.
- **Barcode images** — stored at `./uploads/barcodes/{itemId}.png` relative to the working
  directory. Ensure the folder is writable and back it up separately from the DB.
