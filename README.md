# Sales System - Quarkus Backend

A backend sales system built with Quarkus that manages Users, Products, and Transactions with Keycloak SSO authentication.

## Architecture
- **Framework:** Quarkus (Java 21)
- **Pattern:** Clean Architecture + Repository Pattern + Service Layer
- **Database:** PostgreSQL
- **Authentication:** Keycloak SSO (OAuth2/OIDC)
- **Layers:** Resource (REST API) → Service (Business Logic) → Repository (Data Access) → Entity (JPA)

## Tech Stack
- Quarkus 3.17.0
- Java 21
- PostgreSQL
- Keycloak 26.0 (SSO/OIDC)
- Hibernate ORM 6.6.1 with Panache
- REST (Quarkus REST) + JSON-B
- Bean Validation
- **OIDC Authentication** (`quarkus-oidc`)
- **UUID v7** (`xyz.block:uuidv7:1.1.0`)
- **Swagger UI / OpenAPI 3.1** for API documentation

## Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose (for Keycloak + PostgreSQL)

## Quick Start (Docker Compose)

```bash
# Start all services (PostgreSQL, Keycloak, App)
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f app
docker-compose logs -f keycloak
```

## Services

| Service | URL | Credentials |
|---|---|---|
| App | http://localhost:8080 | N/A |
| Swagger UI | http://localhost:8080/swagger-ui | N/A |
| Keycloak Admin | http://localhost:8180 | admin / admin |
| PostgreSQL | localhost:5432 | nurvan / your_password |

## Keycloak Setup

### Default Users

The Keycloak realm (`sales-realm`) is pre-configured with these users:

| Username | Password | Role |
|---|---|---|
| admin | admin123 | ADMIN |
| cashier1 | cashier123 | CASHIER |
| manager1 | manager123 | MANAGER |

### Adding New Users

1. Open Keycloak Admin Console: http://localhost:8180
2. Select `sales-realm`
3. Go to **Users** → **Create user**
4. Fill in username, set password (uncheck "Temporary")
5. Go to **Role mapping** → **Assign role** → Select client role from `sales-client`

### Customizing the Realm

Edit `keycloak/realm/sales-realm.json` and restart Keycloak:

```bash
docker-compose down
docker-compose up -d
```

## Manual Setup (Without Docker)

### PostgreSQL

```bash
docker run -d --name postgres -e POSTGRES_PASSWORD=your_password -p 5432:5432 postgres:18
docker exec -it postgres psql -U postgres -c "CREATE DATABASE sales_db;"
```

### Keycloak

```bash
docker run -d --name keycloak \
  -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -p 8180:8080 quay.io/keycloak/keycloak:26.0 start-dev
```

Configure Keycloak:
1. Login to http://localhost:8180 (admin/admin)
2. Create realm `sales-realm`
3. Create client `sales-client` (confidential, with secret `your_client_secret`)
4. Add roles: ADMIN, CASHIER, MANAGER, CUSTOMER
5. Create users and assign roles

## Database Setup

### Using PostgreSQL (Docker)

```bash
# Start PostgreSQL container
docker run -d \
  --name postgres \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 \
  postgres:18

# Create database
docker exec -it postgres psql -U postgres -c "CREATE DATABASE sales_db;"

# Create user and grant privileges
docker exec -it postgres psql -U postgres -c "CREATE USER your_user WITH PASSWORD 'your_password';"
docker exec -it postgres psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE sales_db TO your_user;"
docker exec -it postgres psql -U postgres -d sales_db -c "GRANT ALL ON SCHEMA public TO your_user;"
```

```sql
CREATE DATABASE sales_db;
```

## DDL Statements

All entities use **UUID v7** for primary keys. UUID v7 provides time-ordered, sortable identifiers that reduce index fragmentation compared to random UUIDs.

**Implementation:** [`xyz.block:uuidv7`](https://github.com/robsonkades/uuidv7) library (`xyz.block:uuidv7:1.1.0`) generates UUID v7 identifiers directly in entity `@PrePersist` callbacks.

```sql
-- Enable UUID extension (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Products table
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(150) NOT NULL,
    price NUMERIC(15, 2) NOT NULL,
    stock INTEGER NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id UUID NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Transaction Items table
CREATE TABLE transaction_items (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    transaction_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    price NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_transaction_items_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_transaction_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Indexes for better query performance
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transaction_items_transaction_id ON transaction_items(transaction_id);
CREATE INDEX idx_transaction_items_product_id ON transaction_items(product_id);
```

### Schema Notes
- **UUIDv7**: Time-ordered UUIDs generated by `xyz.block.uuidv7.UUIDv7.generate()` in entity `@PrePersist` callbacks
- UUIDv7 provides chronological ordering, reducing index fragmentation compared to random UUIDs
- `users.username` has a unique constraint to prevent duplicate usernames
- Foreign key constraints ensure referential integrity between related tables
- Monetary values use `NUMERIC(15, 2)` for precision (up to 13 digits + 2 decimal places)
- **`created_at`**: Automatically set when record is created via JPA `@PrePersist`
- **`updated_at`**: Automatically updated on every update via JPA `@PreUpdate`
- Cascading deletes: When a transaction is deleted, all its items are automatically deleted (orphanRemoval)

### UUID v7 Format Example
```
019d54a0-6711-767a-8000-042916f916f3
└─────┘ └──┘ └─┘ └────────────────┘
  time   ver  var    random
 (48ms)
```

- First 48 bits: Unix timestamp in milliseconds (time-ordered)
- 4 bits: UUID version (0111 = v7)
- 2 bits: Variant (10 = RFC 4122)
- Remaining 62 bits: Random data

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database Configuration - PostgreSQL
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=your_username
quarkus.datasource.password=your_password
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/sales_db
quarkus.datasource.jdbc.max-size=16
quarkus.datasource.jdbc.min-size=4

# Hibernate ORM Configuration
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=true
quarkus.hibernate-orm.log.format-sql=true
```

## Security

### Keycloak SSO Authentication

All API endpoints are protected by **Keycloak SSO** using OAuth2/OpenID Connect:

- **Protocol:** OpenID Connect (OIDC)
- **Flow:** Authorization Code Flow (web-app) + Direct Access Grants (API)
- **Token:** JWT with roles embedded in `resource_access.sales-client.roles`
- **Session:** Configurable timeout (default: 30 min idle, 10 hours max)

**Authentication Flow:**

```bash
# 1. Get access token from Keycloak
TOKEN=$(curl -s -X POST http://localhost:8180/realms/sales-realm/protocol/openid-connect/token \
  -d "client_id=sales-client" \
  -d "client_secret=your_client_secret" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123" | jq -r '.access_token')

# 2. Use token to access API
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users

# 3. Check user info
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/auth/userinfo
```

**Auth Endpoints:**

| Endpoint | Description |
|---|---|
| `/auth/userinfo` | Get current authenticated user info |
| `/auth/roles` | Get current user's roles |
| `/auth/check` | Check authentication status |
| `/api/*` | All protected by `@Authenticated` |

**Roles:**
- `ADMIN` - Full access to all operations
- `MANAGER` - Manage products and transactions
- `CASHIER` - Create transactions only
- `CUSTOMER` - View products

## Running the Application

### Development Mode
```bash
./mvnw quarkus:dev
```

### Build
```bash
./mvnw clean package
```

### Run JAR
```bash
java -jar target/quarkus-app/quarkus-run.jar
```

### Native Build
```bash
./mvnw package -Pnative
```

## API Documentation

This project uses **Swagger UI (OpenAPI)** for interactive API documentation.

### Access Swagger UI

```
http://localhost:8080/swagger-ui
```

### OpenAPI Specification

```
http://localhost:8080/q/openapi
```

All endpoints are fully documented with:
- Operation descriptions
- Request/response schemas
- Parameter details with UUID v7 examples
- Response codes and error handling
- DTO field descriptions and constraints

### Quick Reference

| Resource | Path |
|----------|------|
| Users | `/api/users` |
| Products | `/api/products` |
| Transactions | `/api/transactions` |

For detailed API documentation, use Swagger UI instead of static tables and examples.

## Project Structure

```
sales-system/
├── docker-compose.yml                 # Docker compose for all services
├── keycloak/
│   └── realm/
│       └── sales-realm.json          # Keycloak realm export (users, roles, client)
├── src/main/java/com/sales/
│   ├── config/
│   │   └── OpenAPIConfig.java        # OpenAPI/Swagger configuration
│   ├── entity/
│   │   ├── UserEntity.java           # No password field (managed by Keycloak)
│   │   ├── ProductEntity.java
│   │   ├── TransactionEntity.java
│   │   └── TransactionItemEntity.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── ProductRepository.java
│   │   └── TransactionRepository.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── ProductService.java
│   │   └── TransactionService.java
│   ├── resource/
│   │   ├── AuthResource.java         # Auth endpoints (userinfo, roles, check)
│   │   ├── UserResource.java         # @Authenticated
│   │   ├── ProductResource.java      # @Authenticated
│   │   ├── TransactionResource.java  # @Authenticated
│   │   └── GlobalExceptionMapper.java
│   └── dto/
│       ├── UserDTO.java              # No password field
│       ├── ProductDTO.java
│       ├── TransactionDTO.java
│       ├── TransactionItemDTO.java
│       └── ErrorResponse.java
├── src/main/resources/
│   └── application.properties        # OIDC config + DB config
└── src/test/java/com/sales/
    ├── UuidV7GenerationTest.java     # UUIDv7 unit tests
    ├── EntityIdGenerationTest.java   # Entity ID integration tests
    ├── CrudResourceTest.java         # Full CRUD API tests
    └── UserResourceTest.java         # User API tests
```

## Testing

### Run All Tests
```bash
./mvnw test
```

### Run Specific Test Class
```bash
# UUIDv7 unit tests (no database required)
./mvnw test -Dtest=UuidV7GenerationTest

# Entity ID integration tests (requires database)
./mvnw test -Dtest=EntityIdGenerationTest

# Full CRUD API tests (requires database)
./mvnw test -Dtest=CrudResourceTest

# User API tests (requires database)
./mvnw test -Dtest=UserResourceTest
```

### Test Coverage
| Test Class | Tests | Description |
|---|---|---|
| `UuidV7GenerationTest` | 6 | UUIDv7 generation, uniqueness, ordering, format validation |
| `EntityIdGenerationTest` | 4 | Entity ID generation for all 4 entity types |
| `CrudResourceTest` | 29 | Full CRUD operations for Users, Products, and Transactions |
| `UserResourceTest` | 3 | Basic user API integration tests |
| **Total** | **42** | |

## Credential Masking (Before Push)

Credentials in `application.properties` are automatically masked before pushing to GitHub using the safe push script.

### Quick Push (Recommended)
```bash
./git-push-safe.sh
```

This script automatically:
1. Backs up real credentials to `application.properties.bak`
2. Replaces them with `your_username` / `your_password`
3. Commits and pushes the masked version
4. Restores real credentials locally
5. Commits the restore locally (not pushed)

### Gitignore Rules
The `.gitignore` excludes `*.bak` files to prevent backed-up credentials from being tracked.

## Notes
- **Authentication via Keycloak SSO** - All API endpoints protected by OAuth2/OIDC
  - Users managed in Keycloak Admin Console (http://localhost:8180)
  - JWT tokens with embedded roles for authorization
  - `@Authenticated` annotation on all REST resources
  - `AuthResource` provides `/auth/userinfo`, `/auth/roles`, `/auth/check` endpoints
- **No password field in UserEntity/UserDTO** - Passwords managed entirely by Keycloak
- **All primary keys use UUID v7** - Time-ordered, sortable identifiers via `xyz.block:uuidv7`
- **All entities have `created_at` and `updated_at` timestamps**
  - `created_at`: Auto-set on insert via JPA `@PrePersist`
  - `updated_at`: Auto-updated on every change via JPA `@PreUpdate`
- **All DTOs include `createdAt` and `updatedAt` fields** for API responses
- **API Documentation** - Interactive Swagger UI at `/swagger-ui`
  - OpenAPI 3.1 spec at `/q/openapi`
  - All endpoints documented with request/response schemas
- Stock validation before creating transaction
- Transaction management in service layer
- DTO pattern for API communication
- Global exception handling
- **Quarkus 3.17.0** with Java 21
- **Docker Compose** for local development (PostgreSQL + Keycloak + App)
- **Tests use H2 database + @TestSecurity** for fast, isolated test execution
