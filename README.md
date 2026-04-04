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
- Keycloak 24.0 (SSO/OIDC)
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
docker compose up -d

# Check service status
docker compose ps

# View logs
docker compose logs -f app
docker compose logs -f keycloak
```

## Services

| Service | URL | Credentials |
|---|---|---|
| App | http://localhost:8080 | N/A |
| Swagger UI | http://localhost:8080/swagger-ui | N/A |
| Keycloak Admin | http://localhost:8180 | admin / XXX |
| PostgreSQL | localhost:5432 | XXX / XXX |

## Keycloak Setup

### Version Note

This project uses **Keycloak 24.0**. Versions 25 and 26 have a known bug where user credentials set via the Admin REST API (`/reset-password` or inline `credentials` during user creation) are silently ignored — the API returns `204 No Content` but passwords are never persisted to the database, causing all logins to fail with `"Account is not fully set up"`. Do not upgrade Keycloak past 24.0 until this is resolved.

### Keycloak User Profile Fix

Keycloak 24+ enables a **User Profile** feature by default that marks `email`, `firstName`, and `lastName` as **required** for the `user` role. This causes login to fail with `"Account is not fully set up"` even when passwords are correctly stored, because Keycloak expects users to fill in those fields on first login.

**Fix applied:** The user profile has been updated to remove the `required` constraint from these fields. If you recreate the realm from scratch, run this after Keycloak starts:

```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
  -d "client_id=admin-cli&grant_type=password&username=admin&password=XXX" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

curl -s -X PUT http://localhost:8180/admin/realms/sales-realm/users/profile \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "attributes": [
      {"name":"username","displayName":"${username}","validations":{"length":{"min":3,"max":255},"username-prohibited-characters":{}},"permissions":{"view":["admin","user"],"edit":["admin","user"]}},
      {"name":"email","displayName":"${email}","validations":{"email":{}},"permissions":{"view":["admin","user"],"edit":["admin","user"]}},
      {"name":"firstName","displayName":"${firstName}","validations":{"length":{"max":255}},"permissions":{"view":["admin","user"],"edit":["admin","user"]}},
      {"name":"lastName","displayName":"${lastName}","validations":{"length":{"max":255}},"permissions":{"view":["admin","user"],"edit":["admin","user"]}}
    ]
  }'
```

### Default Users

The Keycloak realm (`sales-realm`) is pre-configured with these users:

| Username | Password | Role |
|---|---|---|
| admin | XXX | ADMIN |
| cashier1 | XXX | CASHIER |
| manager1 | XXX | MANAGER |

### Client Configuration

| Setting | Value |
|---|---|
| Client ID | `sales-client` |
| Client Secret | `XXX` |
| Type | Confidential |
| Direct Access Grants | Enabled |
| Valid Redirect URIs | `http://localhost:8080/*`, `http://localhost:5000/*` |
| Web Origins | `http://localhost:8080`, `http://localhost:5000` |

### Adding New Users

**Via Keycloak Admin Console:**

1. Open Keycloak Admin Console: http://localhost:8180
2. Select `sales-realm`
3. Go to **Users** → **Create user**
4. Fill in username, set password (uncheck "Temporary")
5. Go to **Role mapping** → **Assign role** → Select client role from `sales-client`

**Via Quarkus API:**

```bash
curl -s -X POST http://localhost:5000/api/keycloak/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "securepass",
    "email": "user@example.com",
    "emailVerified": true,
    "roles": ["CASHIER"]
  }'
```

### Keycloak User Management API

A full CRUD API for managing Keycloak users is available at `/api/keycloak/users`.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/keycloak/users` | List all Keycloak users |
| `GET` | `/api/keycloak/users/{id}` | Get user by ID (includes roles) |
| `GET` | `/api/keycloak/users/username/{username}` | Get user by username |
| `POST` | `/api/keycloak/users` | Create user with password and roles |
| `PUT` | `/api/keycloak/users/{id}` | Update user details |
| `PUT` | `/api/keycloak/users/{id}/password` | Reset user password |
| `PUT` | `/api/keycloak/users/{id}/roles` | Assign roles to user |
| `GET` | `/api/keycloak/users/{id}/roles` | Get user roles |
| `GET` | `/api/keycloak/users/roles` | List available roles |
| `DELETE` | `/api/keycloak/users/{id}` | Delete user |

### Customizing the Realm

Edit `keycloak/realm/sales-realm.json` and restart Keycloak:

```bash
docker compose down
docker compose up -d
```

## Manual Setup (Without Docker)

### PostgreSQL

```bash
docker run -d --name postgres -e POSTGRES_PASSWORD=XXX -p 5432:5432 postgres:17
docker exec -it postgres psql -U XXX -d postgres -c "CREATE DATABASE sales_db;"
docker exec -it postgres psql -U XXX -d postgres -c "CREATE DATABASE keycloak_db;"
```

### Keycloak

```bash
docker run -d --name keycloak \
  -e KEYCLOAK_ADMIN=XXX -e KEYCLOAK_ADMIN_PASSWORD=XXX \
  -e KC_DB=postgres \
  -e KC_DB_URL_HOST=<postgres-container-ip> \
  -e KC_DB_URL_PORT=5432 \
  -e KC_DB_URL_DATABASE=keycloak_db \
  -e KC_DB_USERNAME=XXX \
  -e KC_DB_PASSWORD=XXX \
  -p 8180:8080 \
  -v $(pwd)/keycloak/realm:/opt/keycloak/data/import \
  quay.io/keycloak/keycloak:24.0 start-dev --import-realm
```

After Keycloak starts:
1. Apply the [User Profile Fix](#keycloak-user-profile-fix) above
2. Create users and assign roles

## Database Setup

### Using PostgreSQL (Docker)

```bash
# Start PostgreSQL container
docker run -d \
  --name postgres \
  -e POSTGRES_USER=XXX \
  -e POSTGRES_PASSWORD=XXX \
  -p 5432:5432 \
  postgres:17

# Create databases
docker exec -it postgres psql -U XXX -d postgres -c "CREATE DATABASE sales_db;"
docker exec -it postgres psql -U XXX -d postgres -c "CREATE DATABASE keycloak_db;"
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
- **No password column in `users` table** — authentication is handled entirely by Keycloak SSO

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
# Application
quarkus.application.name=sales-system

# HTTP Configuration
quarkus.http.port=5000

# Database Configuration - PostgreSQL
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=XXX
quarkus.datasource.password=XXX
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/sales_db
quarkus.datasource.jdbc.max-size=16
quarkus.datasource.jdbc.min-size=4

# Hibernate ORM Configuration
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=true
quarkus.hibernate-orm.log.format-sql=true

# OIDC Configuration (Keycloak)
quarkus.oidc.auth-server-url=http://localhost:8180/realms/sales-realm
quarkus.oidc.client-id=sales-client
quarkus.oidc.credentials.secret=XXX
quarkus.oidc.application-type=web-app
quarkus.oidc.authentication.scopes=openid,profile,email,roles
quarkus.oidc.roles.role-claim-name=resource_access.${quarkus.oidc.client-id}.roles

# Keycloak Admin API Configuration
keycloak.admin.url=http://localhost:8180
keycloak.admin.realm=master
keycloak.admin.username=XXX
keycloak.admin.password=XXX
keycloak.admin.target-realm=sales-realm
```

### Port Mapping

| Context | Port | Notes |
|---|---|---|
| Quarkus internal | `5000` | `quarkus.http.port` |
| Docker host | `8080` | `docker-compose.yml` maps `8080:5000` |
| Keycloak | `8180` | Maps container `8080` → host `8180` |

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
  -d "client_secret=XXX" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=XXX" | jq -r '.access_token')

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
App will be available at http://localhost:5000

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
| Auth | `/auth/userinfo`, `/auth/roles`, `/auth/check` |
| Users | `/api/users` |
| Products | `/api/products` |
| Transactions | `/api/transactions` |
| Keycloak Users | `/api/keycloak/users` |

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
│   │   ├── TransactionService.java
│   │   └── KeycloakAdminClient.java  # Keycloak Admin API client
│   ├── resource/
│   │   ├── AuthResource.java         # Auth endpoints (userinfo, roles, check)
│   │   ├── UserResource.java         # @Authenticated
│   │   ├── ProductResource.java      # @Authenticated
│   │   ├── TransactionResource.java  # @Authenticated
│   │   ├── KeycloakUserResource.java # Keycloak user management API
│   │   └── GlobalExceptionMapper.java
│   └── dto/
│       ├── UserDTO.java              # No password field
│       ├── ProductDTO.java
│       ├── TransactionDTO.java
│       ├── TransactionItemDTO.java
│       ├── KeycloakUserDTO.java      # Keycloak user create/update DTO
│       └── ErrorResponse.java
├── src/main/resources/
│   └── application.properties        # OIDC config + DB config
└── src/test/java/com/sales/
    ├── UuidV7GenerationTest.java              # UUIDv7 unit tests
    ├── EntityIdGenerationTest.java            # Entity ID integration tests
    ├── CrudResourceTest.java                  # Full CRUD API tests
    ├── UserResourceTest.java                  # User API tests
    ├── KeycloakAdminClientTest.java           # KeycloakAdminClient service tests
    ├── KeycloakUserResourceTest.java          # Keycloak user management API tests
    ├── UserSyncServiceTest.java               # User sync (Keycloak ↔ Local DB) tests
    ├── InsertAndVerify100ProductsTest.java    # Product creation with user tracking tests
    └── CreateUserEndpointTest.java            # Keycloak user creation endpoint tests
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
| `KeycloakAdminClientTest` | 12 | KeycloakAdminClient service layer tests |
| `KeycloakUserResourceTest` | 13 | Keycloak user management API endpoint tests |
| `UserSyncServiceTest` | 6 | Keycloak ↔ Local DB user synchronization tests |
| `InsertAndVerify100ProductsTest` | 3 | Product creation with user tracking verification |
| `CreateUserEndpointTest` | 4 | Keycloak user creation endpoint tests |
| **Total** | **80** | |

### Test Structure

**Service Layer Tests (`KeycloakAdminClientTest`)**
- Tests all methods in the Keycloak Admin API client
- Handles both scenarios: Keycloak available and unavailable
- Validates error handling (404, 400, 500 status codes)
- Includes user creation with cleanup and role assignment
- Tests role management (get, assign, list available)

**Resource Layer Tests (`KeycloakUserResourceTest`)**
- Tests all REST endpoints for Keycloak user management
- Validates request payloads (password required, roles required)
- Tests error scenarios (missing fields, non-existent users)
- Uses flexible status code matchers for different environments
- Includes security context with `@TestSecurity`

## Credential Management

Credentials are **never committed** to the repository. The following files are excluded via `.gitignore`:

- `src/main/resources/application.properties` — Contains database passwords and OIDC secrets
- `docker-compose.yml` — Contains actual passwords for PostgreSQL and Keycloak

These files must be configured locally from the `.example` templates:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
cp docker-compose.yml.example docker-compose.yml
# Edit with your actual credentials
```

## Keycloak-User Synchronization

When a user is created in Keycloak, a corresponding record is automatically created in the local `users` table.

### How It Works

**On User Creation (`POST /api/keycloak/users`):**
1. User created in Keycloak via Admin API
2. Password set via Keycloak reset-password endpoint
3. Roles assigned (if they exist in Keycloak)
4. `UserSyncService` creates local `UserEntity` with `keycloakId`
5. Returns `UserDTO` with both local UUID v7 ID and Keycloak UUID

**On First Login (via `UserSyncFilter`):**
1. User authenticates via Keycloak OIDC
2. JWT token validated by `quarkus-oidc`
3. `UserSyncFilter` intercepts request after authentication
4. Looks up local user by Keycloak username
5. If not found → auto-creates local user record
6. Request continues to endpoint

### User Entity Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Local UUID v7 primary key |
| `keycloakId` | String | Keycloak user UUID (links to Keycloak) |
| `username` | String | Username (from Keycloak) |
| `role` | String | Role assigned from Keycloak roles |
| `createdAt` | LocalDateTime | Record creation timestamp |
| `updatedAt` | LocalDateTime | Record last update timestamp |

### Configuration

```properties
# Enable/disable auto-sync on login (default: true)
keycloak.admin.sync-users-on-login=true
```

## Product Audit Fields

Products track who created and last updated them via `createdBy` and `updatedBy` fields.

### Product Entity Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Product UUID v7 primary key |
| `name` | String | Product name |
| `price` | BigDecimal | Product price |
| `stock` | Integer | Available stock quantity |
| `createdBy` | UUID | User who created the product |
| `updatedBy` | UUID | User who last updated the product |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp |

### How User Tracking Works

1. Authenticated user calls `POST /api/products` or `PUT /api/products/{id}`
2. `ProductResource` extracts username from `SecurityIdentity`
3. Looks up local user by Keycloak username via `UserService.findByKeycloakId()`
4. If user doesn't exist → auto-creates via `createOrUpdateFromKeycloak()`
5. Local user's UUID stored in `createdBy` / `updatedBy`

## Notes
- **Authentication via Keycloak SSO** - All API endpoints protected by OAuth2/OIDC
  - Users managed in Keycloak Admin Console (http://localhost:8180) or via `/api/keycloak/users` API
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
- **Keycloak 24.0** — Do not upgrade to 25/26 (credential persistence bug)
- **Quarkus app listens on port 5000** internally, Docker maps to host port 8080

## API Response Format

All API responses follow a unified `ApiResponse<T>` wrapper with HTTP status codes:

### Success Response Format

**200 OK (GET, PUT):**
```json
{
  "success": true,
  "status": 200,
  "data": { /* response payload */ },
  "message": null
}
```

**201 Created (POST):**
```json
{
  "success": true,
  "status": 201,
  "data": { /* created resource */ },
  "message": "Resource created"
}
```

**204 No Content (DELETE):**
- Returns empty body (no response wrapper)

### Error Response Format

```json
{
  "success": false,
  "status": 400,  // or 401, 404, 409, 500
  "data": null,
  "message": "Descriptive error message"
}
```

### Response Wrapper Implementation

- **`ApiResponse<T>`** - Generic response wrapper in `com.sales.dto.ApiResponse`
- **`ApiResponseFilter`** - JAX-RS filter that automatically wraps 2xx success responses
- **Exception Mappers** - Convert exceptions to proper `ApiResponse` error responses
- **Status Codes** - All responses include HTTP status code in the `status` field

### Common Status Codes

| Status | Usage |
|--------|-------|
| 200 | Successful GET/PUT operations |
| 201 | Successful POST (resource created) |
| 204 | Successful DELETE (no content) |
| 400 | Bad request, validation errors, insufficient stock |
| 401 | Unauthorized (not authenticated) |
| 404 | Resource not found |
| 409 | Conflict (duplicate username) |
| 500 | Internal server error |

### Test Considerations

When writing tests against the API, access response data through the `data` field:

```java
// Correct - access nested data field
.body("data.username", equalTo("testuser"))
.body("data.id", notNullValue())

// Incorrect - direct field access (old format)
.body("username", equalTo("testuser"))
```

Error responses can be validated by status code:

```java
.statusCode(400)
.statusCode(404)
.statusCode(409)
.statusCode(500)
```
