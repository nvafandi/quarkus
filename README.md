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
| Keycloak Admin | http://localhost:8180 | <your_admin_username> / <your_admin_password> |
| PostgreSQL | localhost:5432 | <your_db_username> / <your_db_password> |

## Keycloak Setup

### Version Note

This project uses **Keycloak 24.0**. Versions 25 and 26 have a known bug where user credentials set via the Admin REST API (`/reset-password` or inline `credentials` during user creation) are silently ignored — the API returns `204 No Content` but passwords are never persisted to the database, causing all logins to fail with `"Account is not fully set up"`. Do not upgrade Keycloak past 24.0 until this is resolved.

### Keycloak User Profile Fix

Keycloak 24+ enables a **User Profile** feature by default that marks `email`, `firstName`, and `lastName` as **required** for the `user` role. This causes login to fail with `"Account is not fully set up"` even when passwords are correctly stored, because Keycloak expects users to fill in those fields on first login.

**Fix applied:** The user profile has been updated to remove the `required` constraint from these fields. If you recreate the realm from scratch, run this after Keycloak starts:

```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
  -d "client_id=admin-cli&grant_type=password&username=<your_admin_username>&password=<your_admin_password>" \
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
| admin | <your_admin_password> | ADMIN |
| cashier1 | <your_cashier_password> | CASHIER |
| manager1 | <your_manager_password> | MANAGER |
| <your_username> | <your_user_password> | ADMIN |

### Client Configuration

| Setting | Value |
|---|---|
| Client ID | `sales-client` |
| Client Secret | `<your_client_secret>` |
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
curl -s -X POST http://localhost:5000/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "securepass",
    "email": "user@example.com",
    "roles": ["CASHIER"]
  }'
```

**Request body fields:**

| Field | Required | Description |
|---|---|---|
| `username` | Yes | Unique login name |
| `password` | Yes | User password |
| `email` | No | User email |
| `roles` | No | Client roles (e.g. `["ADMIN"]`, `["CASHIER"]`) |

**System-managed fields** (auto-set, not accepted in request body):
- `id` — Keycloak-generated UUID
- `emailVerified` — defaults to `true`
- `enabled` — defaults to `true`
- `createdTimestamp` — set by system on creation

### User Management API

All user operations (Keycloak + local DB) are available at `/api/users`.

**Keycloak User Endpoints:**

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/users` | List all Keycloak users |
| `GET` | `/api/users/{id}` | Get user by Keycloak ID (includes roles) |
| `GET` | `/api/users/username/{username}` | Get user by username |
| `GET` | `/api/users/roles` | List available roles |
| `POST` | `/api/users` | Create user with password and roles |
| `PUT` | `/api/users/{id}` | Update Keycloak user details |
| `PUT` | `/api/users/{id}/password` | Reset user password |
| `PUT` | `/api/users/{id}/roles` | Assign roles to user |
| `GET` | `/api/users/{id}/roles` | Get user roles |
| `DELETE` | `/api/users/{id}` | Delete user from Keycloak and local DB |

### Customizing the Realm

Edit `keycloak/realm/sales-realm.json` and restart Keycloak:

```bash
docker compose down
docker compose up -d
```

## Manual Setup (Without Docker)

### PostgreSQL

```bash
docker run -d --name postgres -e POSTGRES_PASSWORD=<your_db_password> -p 5432:5432 postgres:17
docker exec -it postgres psql -U <your_db_username> -d postgres -c "CREATE DATABASE sales_db;"
docker exec -it postgres psql -U <your_db_username> -d postgres -c "CREATE DATABASE keycloak_db;"
```

### Keycloak

```bash
docker run -d --name keycloak \
  -e KEYCLOAK_ADMIN=<your_admin_username> -e KEYCLOAK_ADMIN_PASSWORD=<your_admin_password> \
  -e KC_DB=postgres \
  -e KC_DB_URL_HOST=<postgres-container-ip> \
  -e KC_DB_URL_PORT=5432 \
  -e KC_DB_URL_DATABASE=keycloak_db \
  -e KC_DB_USERNAME=<your_db_username> \
  -e KC_DB_PASSWORD=<your_db_password> \
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
  -e POSTGRES_USER=<your_db_username> \
  -e POSTGRES_PASSWORD=<your_db_password> \
  -p 5432:5432 \
  postgres:17

# Create databases
docker exec -it postgres psql -U <your_db_username> -d postgres -c "CREATE DATABASE sales_db;"
docker exec -it postgres psql -U <your_db_username> -d postgres -c "CREATE DATABASE keycloak_db;"
```

## DDL Statements

All entities use **UUID v7** for primary keys. UUID v7 provides time-ordered, sortable identifiers that reduce index fragmentation compared to random UUIDs.

**Implementation:** [`xyz.block:uuidv7`](https://github.com/robsonkades/uuidv7) library (`xyz.block:uuidv7:1.1.0`) generates UUID v7 identifiers directly in entity `@PrePersist` callbacks.

```sql
-- Enable UUID extension (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    keycloak_id VARCHAR(255) UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Products table
CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    price NUMERIC(15, 2) NOT NULL,
    stock INTEGER NOT NULL,
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Transaction Items table
CREATE TABLE transaction_items (
    id UUID PRIMARY KEY,
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
CREATE INDEX idx_users_keycloak_id ON users(keycloak_id);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transaction_items_transaction_id ON transaction_items(transaction_id);
CREATE INDEX idx_transaction_items_product_id ON transaction_items(product_id);
```

### Schema Notes
- **UUIDv7**: Time-ordered UUIDs generated by `xyz.block.uuidv7.UUIDv7.generate()` in entity `@PrePersist` callbacks
- UUIDv7 provides chronological ordering, reducing index fragmentation compared to random UUIDs
- **Users table**:
  - `keycloak_id`: Links local user to Keycloak user (unique, nullable)
  - `username`: Unique constraint to prevent duplicate usernames
  - No password column — authentication handled by Keycloak SSO
- **Products table**:
  - `created_by`: UUID of user who created the product (from JWT token)
  - `updated_by`: UUID of user who last updated the product (from JWT token)
  - Both are read-only in API responses
- Foreign key constraints ensure referential integrity between related tables
- Monetary values use `NUMERIC(15, 2)` for precision (up to 13 digits + 2 decimal places)
- **`created_at`**: Automatically set when record is created via JPA `@PrePersist`
- **`updated_at`**: Automatically updated on every change via JPA `@PreUpdate`
- Cascading deletes: When a transaction is deleted, all its items are automatically deleted (`orphanRemoval`)

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
quarkus.datasource.username=<your_db_username>
quarkus.datasource.password=<your_db_password>
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
quarkus.oidc.credentials.secret=<your_client_secret>
quarkus.oidc.application-type=hybrid
quarkus.oidc.authentication.scopes=openid profile email roles
quarkus.oidc.roles.role-claim-name=resource_access.${quarkus.oidc.client-id}.roles

# Keycloak Admin API Configuration
keycloak.admin.url=http://localhost:8180
keycloak.admin.realm=master
keycloak.admin.username=<your_admin_username>
keycloak.admin.password=<your_admin_password>
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
  -d "client_secret=<your_client_secret>" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=<your_admin_password>" | jq -r '.access_token')

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

### Token Management API

A simplified token management API is available for direct authentication without browser-based flow:

| Method | Path | Description |
|---|---|---|
| `POST` | `/auth/login` | Login with username/password, get access + refresh tokens |
| `POST` | `/auth/refresh` | Use refresh token to get new access token |
| `POST` | `/auth/revoke` | Revoke refresh token to invalidate session |

**Login Example:**

```bash
# 1. Login - get access and refresh tokens
curl -s -X POST http://localhost:5000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "<your_admin_password>"
  }'

# Response:
{
  "success": true,
  "status": 200,
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 300,
    "refreshExpiresIn": 1800,
    "tokenType": "Bearer",
    "scope": "openid profile email roles"
  }
}
```

**Request Body (Login):**
- `username` (required) - Keycloak username
- `password` (required) - User password

**Response Fields:**
- `accessToken` - JWT access token for API authorization
- `refreshToken` - JWT refresh token for getting new access tokens
- `expiresIn` - Access token expiry in seconds
- `refreshExpiresIn` - Refresh token expiry in seconds
- `tokenType` - Always "Bearer"
- `scope` - Granted scopes

**Using Access Token:**
```bash
curl -H "Authorization: Bearer <accessToken>" http://localhost:5000/api/products
```

**Refresh Token (when expired):**
```bash
curl -s -X POST http://localhost:5000/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refreshToken>"
  }'
```

**Revoke Token (logout):**
```bash
curl -s -X POST http://localhost:5000/auth/revoke \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refreshToken>"
  }'
```

**Error Responses:**
| Scenario | Status | Message |
|---|---|---|
| Invalid username/password | 401 | "Invalid username or password" |
| Expired refresh token | 401 | "Invalid or expired refresh token" |
| Missing fields | 400 | Validation error |

### User Authentication in API Requests

For API calls that require authentication, the system extracts the user's Keycloak UUID from the JWT token's `sub` claim. The `createdBy` and `updatedBy` fields in ProductEntity are automatically set from the authenticated user's token - they cannot be manually specified in the request body.

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
| Auth | `/auth/login`, `/auth/refresh`, `/auth/revoke`, `/auth/userinfo`, `/auth/roles`, `/auth/check` |
| Users | `/api/users` (Keycloak CRUD) |
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
│   ├── auth/
│   │   └── UserSyncFilter.java       # Auto-sync Keycloak user on first login
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
│   │   ├── KeycloakAdminClient.java  # Keycloak Admin API client
│   │   └── UserSyncService.java      # Keycloak ↔ Local DB sync
│   ├── resource/
│   │   ├── AuthResource.java         # Auth + token management (login, refresh, revoke, userinfo, roles, check)
│   │   ├── UserResource.java         # Keycloak user management + local sync
│   │   ├── ProductResource.java      # @Authenticated
│   │   └── TransactionResource.java  # @Authenticated
│   ├── util/
│   │   └── SecurityContextHelper.java # Shared JWT user ID extraction
│   ├── exception/
│   │   ├── BadRequestException.java      # 400 errors
│   │   ├── ConflictException.java        # 409 errors
│   │   ├── ResourceNotFoundException.java # 404 errors
│   │   ├── ServerErrorException.java     # 500 errors
│   │   ├── UnAuthorizedException.java    # 401 errors
│   │   └── GlobalExceptionMapper.java    # Centralized exception handling
│   └── dto/
│       ├── UserDTO.java              # No password field
│       ├── ProductDTO.java
│       ├── TransactionDTO.java
│       ├── TransactionItemDTO.java
│       ├── TokenDTO.java             # Token response DTO
│       ├── LoginRequestDTO.java      # Login request DTO (username + password)
│       ├── KeycloakUserDTO.java      # Keycloak user create/update DTO
│       └── ApiResponse.java          # Unified response wrapper
├── src/main/resources/
│   └── application.properties        # OIDC config + DB config
└── src/test/java/com/sales/
    ├── AuthResourceTest.java                # Auth + token API tests (13 tests)
    ├── ProductResourceTest.java             # Product CRUD API tests (11 tests)
    ├── TransactionResourceTest.java         # Transaction CRUD API tests (11 tests)
    └── UserResourceTest.java                # Keycloak user management API tests (11 tests)
```

## Testing

### Run All Tests
```bash
./mvnw test
```

### Run Specific Test Class
```bash
# Auth + token tests
./mvnw test -Dtest=AuthResourceTest

# Product CRUD tests
./mvnw test -Dtest=ProductResourceTest

# Transaction CRUD tests
./mvnw test -Dtest=TransactionResourceTest

# User management tests
./mvnw test -Dtest=UserResourceTest
```

### Test Coverage
| Test Class | Tests | Description |
|---|---|---|
| `AuthResourceTest` | 13 | Auth + token login, refresh, revoke, userinfo, roles, check |
| `ProductResourceTest` | 11 | Product CRUD with validation and user tracking |
| `TransactionResourceTest` | 11 | Transaction CRUD with stock validation and user extraction from token |
| `UserResourceTest` | 11 | Keycloak user management API with graceful error handling |
| **Total** | **46** | |

### Test Structure

**Auth Tests (`AuthResourceTest`)**
- Token operations: login, refresh, revoke with validation
- User info operations: userinfo, roles, check with authenticated/anonymous scenarios
- Uses `@TestSecurity` for isolated test execution

**Product Tests (`ProductResourceTest`)**
- Full CRUD: create, read, update, delete
- Validation: missing name, negative price, negative stock
- Verifies auto-set `createdBy`/`updatedBy` from security context

**Transaction Tests (`TransactionResourceTest`)**
- Create with auto-calculated total amount
- Stock validation (insufficient stock returns 400)
- User ID extracted from JWT token via `SecurityContextHelper`
- Delete cascades to transaction items

**User Tests (`UserResourceTest`)**
- Keycloak user management endpoints
- Validation: missing password, missing username, missing roles
- Error handling for non-existent users (404/500)
- Graceful handling when Keycloak is unavailable

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

1. Authenticated user calls `POST /api/products` or `PUT /api/products/{id}` with JWT Bearer token
2. `ProductResource` extracts Keycloak UUID from JWT token's `sub` claim via `JsonWebToken`
3. Looks up local user by `keycloakId` in database
4. If user doesn't exist → auto-creates local user record
5. Local user's UUID stored in `createdBy` / `updatedBy`
6. Request body `createdBy` and `updatedBy` fields are ignored (read-only in responses)

### Product DTO Fields

**Request Body (POST/PUT) - Accepted Fields:**
```json
{
  "name": "Laptop Pro 15",
  "price": 1500000.00,
  "stock": 50
}
```

**Response Body (GET/POST/PUT) - All Fields:**
```json
{
  "id": "019d54c8-...",
  "name": "Laptop Pro 15",
  "price": 1500000.00,
  "stock": 50,
  "createdAt": "2026-04-05T14:00:00",
  "updatedAt": "2026-04-05T14:00:00",
  "createdBy": "59b08f3c-...",
  "updatedBy": "59b08f3c-..."
}
```

Note: `createdBy` and `updatedBy` are read-only. They are automatically set from the authenticated user's JWT token.

## Notes
- **Authentication via Keycloak SSO** - All API endpoints protected by OAuth2/OIDC
  - Users managed in Keycloak Admin Console (http://localhost:8180) or via `/api/users` API
  - JWT tokens with embedded roles for authorization
  - `@Authenticated` annotation on Product and Transaction resources
  - `AuthResource` provides token management (`/auth/login`, `/auth/refresh`, `/auth/revoke`) and user info (`/auth/userinfo`, `/auth/roles`, `/auth/check`) endpoints
  - OIDC application type: `hybrid` (supports both browser flow and Bearer token authentication)
  - User ID extracted from JWT token via shared `SecurityContextHelper` utility
- **No password field in UserEntity/UserDTO** - Passwords managed entirely by Keycloak
- **All primary keys use UUID v7** - Time-ordered, sortable identifiers via `xyz.block:uuidv7`
- **All entities have `created_at` and `updated_at` timestamps**
  - `created_at`: Auto-set on insert via JPA `@PrePersist`
  - `updated_at`: Auto-updated on every change via JPA `@PreUpdate`
- **All DTOs include `createdAt` and `updatedAt` fields** for API responses
- **ProductDTO** - `createdBy` and `updatedBy` are read-only (auto-set from JWT token)
- **API Documentation** - Interactive Swagger UI at `/swagger-ui`
  - OpenAPI 3.1 spec at `/q/openapi`
  - All endpoints documented with request/response schemas
- Stock validation before creating transaction
- Transaction management in service layer
- DTO pattern for API communication
- Global exception handling with unified `ApiResponse<T>` format
- **Quarkus 3.17.0** with Java 21
- **Docker Compose** for local development (PostgreSQL + Keycloak + App)
- **Tests use H2 database + @TestSecurity** for fast, isolated test execution
- **Keycloak 24.0** — Do not upgrade to 25/26 (credential persistence bug)
- **Quarkus app listens on port 5000** internally, Docker maps to host port 8080
- **OIDC scopes**: `openid profile email roles` (space-separated, not comma-separated)

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

## Global Exception Handling

All exceptions are handled centrally by `GlobalExceptionMapper`, which converts them to unified `ApiResponse` error responses.

### Custom Exception Classes

| Exception | HTTP Status | Usage |
|---|---|---|
| `BadRequestException` | 400 | Validation errors, missing required fields, bad input |
| `UnAuthorizedException` | 401 | Authentication failures (invalid credentials, expired tokens) |
| `ResourceNotFoundException` | 404 | Requested resource does not exist |
| `ConflictException` | 409 | Duplicate resource (e.g. username already exists) |
| `ServerErrorException` | 500 | Internal server errors, external service failures |

### Error Response Format

```json
{
  "success": false,
  "status": 400,
  "data": null,
  "message": "Password is required"
}
```

## DTO Field Conventions

Fields that are auto-generated by the system are marked with `@JsonProperty(access = READ_ONLY)` and should not be sent in request bodies.

### KeycloakUserDTO (POST /api/users)

**Request body (send):**
- `username` (required)
- `password` (required)
- `email` (optional)
- `roles` (optional)

**System-managed (do not send):**
- `id` — generated by Keycloak
- `emailVerified` — defaults to `true`
- `enabled` — defaults to `true`
- `createdTimestamp` — set by Keycloak on creation

### UserDTO (Local DB)

**Request body (send):**
- `username` (required)
- `role` (optional)

**System-managed (do not send):**
- `id` — auto-generated UUID
- `keycloakId` — set during Keycloak sync
- `createdAt` — auto-set on creation
- `updatedAt` — auto-set on every update

### ProductDTO

**Request body (send):**
- `name` (required)
- `price` (required)
- `stock` (required)

**System-managed (do not send):**
- `id` — auto-generated UUID
- `createdBy` — extracted from JWT token
- `updatedBy` — extracted from JWT token
- `createdAt` / `updatedAt` — auto-set

### TransactionDTO

**Request body (send):**
- `items` (required, list of `productId` + `quantity`)

**System-managed (do not send):**
- `id` — auto-generated UUID
- `userId` — extracted from JWT token via `SecurityContextHelper`
- `totalAmount` — auto-calculated from items
- `createdAt` / `updatedAt` — auto-set

### TransactionItemDTO

**Request body (send):**
- `productId` (required)
- `quantity` (required)

**System-managed (do not send):**
- `price` — auto-set from product price at time of purchase

## Environment Configuration

All sensitive credentials are stored in `.env` file which is **NOT tracked by git** (see `.gitignore`).

### Setup Environment Variables

1. **Copy template file:**
```bash
cp .env.example .env
```

2. **Edit `.env` with your credentials:**
```bash
# .env file (DO NOT commit to git!)

# PostgreSQL
POSTGRES_DB=sales_db
POSTGRES_USER=your_username
POSTGRES_PASSWORD=your_secure_password

# Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=your_admin_password
KEYCLOAK_DB_DATABASE=keycloak_db
KEYCLOAK_DB_USERNAME=your_username
KEYCLOAK_DB_PASSWORD=your_secure_password

# Quarkus App
QUARKUS_DATASOURCE_USERNAME=your_username
QUARKUS_DATASOURCE_PASSWORD=your_secure_password
QUARKUS_OIDC_CLIENT_ID=sales-client
QUARKUS_OIDC_CREDENTIALS_SECRET=<get_from_keycloak>

# Ports & Container Names (optional)
POSTGRES_PORT=5432
KEYCLOAK_PORT=8180
APP_PORT=8080
POSTGRES_IMAGE_VERSION=18
KEYCLOAK_IMAGE_VERSION=24.0
```

3. **Get Keycloak Client Secret:**
   - Start Docker Compose: `docker-compose up -d`
   - Open Keycloak Admin Console: http://localhost:8180
   - Login with: `admin` / `admin` (from `.env`)
   - Navigate: **sales-realm** → **Clients** → **sales-client** → **Credentials**
   - Copy the **Client Secret** and update `QUARKUS_OIDC_CREDENTIALS_SECRET` in `.env`
   - Restart app: `docker-compose restart app`

### Environment Variables Reference

| Variable | Description | Default Value | Notes |
|---|---|---|---|
| `POSTGRES_DB` | Database name | `sales_db` | Auto-created |
| `POSTGRES_USER` | DB admin username | `your_username` | ⚠️ Change for production |
| `POSTGRES_PASSWORD` | DB admin password | `your_secure_password` | ⚠️ Use strong password |
| `KEYCLOAK_ADMIN` | Keycloak admin user | `admin` | Keycloak master realm |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin password | `your_admin_password` | ⚠️ Use strong password |
| `KEYCLOAK_DB_DATABASE` | Keycloak's database | `keycloak_db` | Separate from sales_db |
| `KEYCLOAK_DB_USERNAME` | Keycloak DB user | Same as `POSTGRES_USER` | - |
| `KEYCLOAK_DB_PASSWORD` | Keycloak DB password | Same as `POSTGRES_PASSWORD` | - |
| `QUARKUS_DATASOURCE_USERNAME` | App DB username | Same as `POSTGRES_USER` | - |
| `QUARKUS_DATASOURCE_PASSWORD` | App DB password | Same as `POSTGRES_PASSWORD` | - |
| `QUARKUS_OIDC_CLIENT_ID` | OAuth2 client ID | `sales-client` | Defined in Keycloak realm |
| `QUARKUS_OIDC_CREDENTIALS_SECRET` | OAuth2 client secret | `<get_from_keycloak>` | ⚠️ CRITICAL - Must match Keycloak |
| `POSTGRES_PORT` | PostgreSQL port (host) | `5432` | Optional - for custom port |
| `KEYCLOAK_PORT` | Keycloak port (host) | `8180` | Optional - for custom port |
| `APP_PORT` | App port (host) | `8080` | Optional - for custom port |
| `QUARKUS_INTERNAL_PORT` | App port (container) | `5000` | Do not change |
| `POSTGRES_IMAGE_VERSION` | PostgreSQL version | `18` | Optional - for version selection |
| `KEYCLOAK_IMAGE_VERSION` | Keycloak version | `24.0` | ⚠️ Do not change (see README) |

### How Docker Compose Reads .env

When you run `docker-compose up`, it automatically:

1. Loads variables from `.env` file (if exists)
2. Substitutes `${VARIABLE_NAME}` placeholders in `docker-compose.yml`
3. Creates containers with actual values

**Example variable substitution:**

```yaml
# In docker-compose.yml:
environment:
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}

# With .env containing: POSTGRES_PASSWORD=kmzwa88saa
# Results in:
environment:
  POSTGRES_PASSWORD: kmzwa88saa
```

### Security Best Practices

✅ **DO:**
- Store `.env` in `.gitignore` (already done)
- Use strong passwords (12+ characters, mixed case, numbers, symbols)
- Keep `.env` local (never commit to git)
- Use `.env.example` as template
- Rotate credentials regularly
- Use secrets management for production (Vault, AWS Secrets Manager)

❌ **DON'T:**
- Commit `.env` to git
- Share `.env` in messages or emails
- Use simple passwords (like "admin")
- Hard-code credentials in docker-compose.yml
- Use same password for all services
- Commit credentials anywhere in version control

### Troubleshooting

**Issue: "Could not resolve placeholder in string"**
```bash
❌ Error: docker-compose config
✅ Solution: Create .env file with all required variables
```

**Issue: "psql: error: connection refused"**
```bash
❌ Cause: .env credentials don't match (POSTGRES_USER/PASSWORD mismatch)
✅ Solution: Verify .env values match across services
```

**Issue: "Keycloak password not working"**
```bash
❌ Cause: Wrong KEYCLOAK_ADMIN_PASSWORD in .env
✅ Solution: Ensure password matches in .env before first startup
```

