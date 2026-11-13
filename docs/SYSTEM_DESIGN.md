# System Design

## 1. Overview

Stateless REST API for an e-commerce storefront. Clients authenticate with a JWT; every request is self-contained, so the API scales horizontally behind a load balancer without sticky sessions.

```
            ┌────────────┐
Client ───▶ │ Load balancer │ ───▶ [ API instance 1..N (Spring Boot, stateless) ]
                                          │
                                          ▼
                                   PostgreSQL (primary)
                                   Flyway migrations
```

## 2. Data model

```
users 1─────1 carts 1───* cart_items *───1 products *───1 categories
users 1───* orders 1───* order_items *───1 products
```

| Table | Purpose | Key columns |
|---|---|---|
| `users` | Accounts and roles (`USER`/`ADMIN`) | `email` (unique), `password` (BCrypt hash), `role` |
| `categories` | Product grouping | `name` (unique) |
| `products` | Catalog items | `price NUMERIC(12,2)`, `stock_quantity`, `category_id` FK |
| `carts` | One active cart per user | `user_id` (unique FK) |
| `cart_items` | Cart lines | `cart_id`, `product_id`, `quantity > 0` |
| `orders` | Purchases with status machine | `user_id`, `status`, `total_amount` |
| `order_items` | **Price snapshot** at purchase time | `order_id`, `product_id`, `quantity`, `unit_price` |

Design decisions:

- **Price snapshotting**: `order_items.unit_price` copies the product price at checkout, so later price changes never mutate historical orders.
- **Money as `NUMERIC(12,2)`** mapped to `BigDecimal` — never floating point.
- **Cart persistence**: the cart survives sessions and devices; checkout clears it in one transaction.
- **Indexes** on hot foreign keys (`products.category_id`, `orders.user_id`, …) and product name search.

## 3. Order lifecycle

```
PENDING ──▶ PAID ──▶ SHIPPED ──▶ DELIVERED
   │          │
   └── CANCELLED (allowed while PENDING or PAID)
```

Checkout performs, inside one transaction: stock validation → order + order_items creation → stock decrement → cart clearing. Any failure rolls the whole thing back.

## 4. API reference

### Auth — `/api/v1/auth` (public)
| Method | Path | Description |
|---|---|---|
| POST | `/register` | Create account (`USER` role) |
| POST | `/login` | Exchange credentials for JWT |

### Catalog — public reads
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/products?categoryId&minPrice&maxPrice&search&page&size&sort` | Filtered, paginated search |
| GET | `/api/v1/products/{id}` | Product detail |
| GET | `/api/v1/categories` · `/api/v1/categories/{id}` | Category list/detail |

### Cart — `/api/v1/cart` (authenticated)
| Method | Path | Description |
|---|---|---|
| GET | `/` | Current cart with totals |
| POST | `/items` | Add product (validates stock) |
| PUT | `/items/{productId}` | Change quantity |
| DELETE | `/items/{productId}` | Remove line |
| DELETE | `/` | Clear cart |

### Orders — `/api/v1/orders` (authenticated)
| Method | Path | Description |
|---|---|---|
| POST | `/checkout` | Convert cart into an order |
| GET | `/` | My orders (paginated) |
| GET | `/{id}` | My order detail (admins can view any) |

### Admin — `/api/v1/admin` (`ADMIN` role)
| Method | Path | Description |
|---|---|---|
| POST/PUT/DELETE | `/products`, `/products/{id}` | Product CRUD |
| POST/DELETE | `/categories`, `/categories/{id}` | Category management |
| GET | `/orders` | All orders |
| PUT | `/orders/{id}/status` | Transition order status |

## 5. Security

- **Stateless JWT** (HS256, 24h expiry). The filter chain authenticates `Authorization: Bearer <token>` on every request; no server-side session exists.
- **Passwords** hashed with BCrypt (strength 10 default).
- **Layered authorization**: coarse route rules in `SecurityConfig` (e.g. `/api/v1/admin/**` needs `ROLE_ADMIN`), plus `@PreAuthorize` on the admin controller.
- **Ownership checks** in `OrderService.findMyOrder` — users see only their own orders; a missing record and a forbidden record both return 404 to avoid leaking existence.
- **Error contract**: all errors return a consistent `{status, error, message, timestamp}` JSON body; validation errors include per-field messages.

## 6. Scaling notes (beyond v0)

- **Read scaling**: product browsing is read-heavy — add a read replica or Redis cache for catalog queries.
- **Stock contention**: at high concurrency, move stock decrement to `UPDATE ... SET stock = stock - ? WHERE stock >= ?` (optimistic row lock) instead of entity checks.
- **Checkout reliability**: introduce idempotency keys so retried checkouts never double-create orders.
- **Payments**: `PAID` should be set by a payment-provider webhook, not by trust.
- **Search**: replace `LIKE` search with PostgreSQL full-text or Elasticsearch when the catalog grows.
- **Observability**: add Actuator + Micrometer metrics, structured logs, and request tracing.

## 7. Testing strategy

- **Unit tests** (present): JWT generation/tampering, cart business rules (accumulation, stock limits, cart creation).
- **Integration tests** (suggested next): `@SpringBootTest` + Testcontainers against a real PostgreSQL, covering auth flows and checkout transactions.
- **Contract/API tests**: springdoc keeps the OpenAPI spec authoritative; use it to generate client stubs.

## 8. Deployment

`docker compose up --build` runs API + PostgreSQL. The Dockerfile is a multi-stage build (Maven build → JRE runtime) for a small runtime image. Configure `DB_*` and `JWT_SECRET` via environment variables; never commit production secrets.
