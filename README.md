# E-commerce Backend

A production-shaped e-commerce REST API built from scratch with **Java 17, Spring Boot 3, and PostgreSQL**.

## What's inside

- **Products & categories** — public browsing with search, category/price filters, and pagination; full CRUD for admins
- **Authentication** — register/login with BCrypt-hashed passwords and stateless JWT authorization
- **Authorization** — `USER` and `ADMIN` roles enforced at the route and method level
- **Shopping cart** — per-user cart with add/update/remove/clear, stock checks, and live totals
- **Orders** — one-click checkout that snapshots prices, decrements stock, and supports status transitions (`PENDING → PAID → SHIPPED → DELIVERED`, cancellable while pending/paid)
- **Database** — schema and seed data managed by Flyway migrations; JPA validates against them
- **API docs** — Swagger UI at `/swagger-ui.html` (springdoc-openapi)
- **Tests** — unit tests for JWT handling and cart business logic (no Docker required)

## Architecture

Layered architecture: `Controller → Service → Repository → Database`.

```
src/main/java/com/example/ecommerce/
├── controller/     # REST layer: request mapping, validation, HTTP semantics
├── dto/            # Request/response records; nothing internal leaks out
├── domain/         # Enums: Role, OrderStatus
├── entity/         # JPA entities: User, Product, Category, Cart, CartItem, Order, OrderItem
├── exception/     # Global error handling with consistent JSON error bodies
├── repository/     # Spring Data JPA repositories
├── security/      # JWT service, auth filter, Spring Security configuration
└── service/        # Business logic: auth, products, categories, cart, orders
```

See [docs/SYSTEM_DESIGN.md](docs/SYSTEM_DESIGN.md) for the full system design: data model, API reference, and scaling notes.

## Prerequisites

- Docker (easiest) **or** Java 17+ and a PostgreSQL instance

## Run with Docker

```bash
docker compose up --build
```

The API starts on `http://localhost:8080`, backed by a PostgreSQL container. Flyway applies migrations and seed data on startup.

## Run locally (no Docker)

1. Start PostgreSQL and create a database:
   ```sql
   CREATE DATABASE ecommerce;
   ```
2. Export connection settings (defaults shown):
   ```bash
   export DB_URL="jdbc:postgresql://localhost:5432/ecommerce"
   export DB_USERNAME="your_db_user"
   export DB_PASSWORD="your_db_password"
   export JWT_SECRET="a-secret-that-is-at-least-32-bytes-long"
   ```
3. Run the app:
   ```bash
   ./mvnw spring-boot:run   # or: mvn spring-boot:run
   ```

## Try it

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"secretpass","fullName":"Jane Doe"}'

# Login (returns a JWT)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"secretpass"}'

# Browse products (public)
curl "http://localhost:8080/api/v1/products?search=head&minPrice=100"

# Add to cart (use the token from login)
curl -X POST http://localhost:8080/api/v1/cart/items \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

# Checkout
curl -X POST http://localhost:8080/api/v1/orders/checkout \
  -H "Authorization: Bearer <TOKEN>"
```

## Run tests

```bash
mvn test
```

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ecommerce` | JDBC connection string |
| `DB_USERNAME` / `DB_PASSWORD` | `ecommerce` / `ecommerce` | Database credentials |
| `JWT_SECRET` | dev-only fallback | Signing key (min 32 bytes; **must** be set in production) |
| `app.jwt.expiration-ms` | `86400000` | Token lifetime (24h) |

## Notes

- `spring.jpa.hibernate.ddl-auto=validate` — the Flyway schema is the single source of truth.
- Admin endpoints live under `/api/v1/admin/**` and require an `ADMIN` role. Seed an admin by promoting a registered user directly in the database.
