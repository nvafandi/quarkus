# Keycloak User API - cURL Examples

Base URL: `http://localhost:5000/api/keycloak/users`

---

## 1. Create User

Creates a new user in Keycloak and syncs to local database.

```bash
curl -X POST http://localhost:5000/api/keycloak/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePass123!",
    "email": "john@example.com",
    "emailVerified": true,
    "roles": ["USER"]
  }'
```

**Response (201 Created):**
```json
{
  "id": "019d5a11-67e9-73fc-a91f-65b008cff84b",
  "keycloakId": "e0f94514-b71a-4c06-beaf-4c764555e9f9",
  "username": "john_doe",
  "role": "USER",
  "createdAt": "2026-04-05T03:00:00",
  "updatedAt": "2026-04-05T03:00:00"
}
```

**Error Responses:**
- `400` - Password missing or invalid
- `409` - Username already exists
- `500` - Keycloak unavailable

---

## 2. List All Users

Returns all users from Keycloak.

```bash
curl -X GET http://localhost:5000/api/keycloak/users
```

**Response (200 OK):**
```json
[
  {
    "id": "e0f94514-b71a-4c06-beaf-4c764555e9f9",
    "username": "john_doe",
    "email": "john@example.com",
    "enabled": true,
    "emailVerified": true
  }
]
```

---

## 3. Get User by ID

Returns a single user from Keycloak by Keycloak UUID.

```bash
curl -X GET http://localhost:5000/api/keycloak/users/{keycloak-id}
```

**Example:**
```bash
curl -X GET http://localhost:5000/api/keycloak/users/e0f94514-b71a-4c06-beaf-4c764555e9f9
```

**Response (200 OK):**
```json
{
  "id": "e0f94514-b71a-4c06-beaf-4c764555e9f9",
  "username": "john_doe",
  "email": "john@example.com",
  "enabled": true,
  "roles": ["USER"]
}
```

**Error Responses:**
- `404` - User not found
- `500` - Keycloak unavailable

---

## 4. Get User by Username

Returns a single user from Keycloak by username.

```bash
curl -X GET http://localhost:5000/api/keycloak/users/username/{username}
```

**Example:**
```bash
curl -X GET http://localhost:5000/api/keycloak/users/username/john_doe
```

**Response (200 OK):**
```json
{
  "id": "e0f94514-b71a-4c06-beaf-4c764555e9f9",
  "username": "john_doe",
  "email": "john@example.com",
  "enabled": true,
  "roles": ["USER"]
}
```

---

## 5. Update User

Updates user details in Keycloak (username, email, enabled status).

```bash
curl -X PUT http://localhost:5000/api/keycloak/users/{keycloak-id} \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe_updated",
    "email": "john.updated@example.com",
    "emailVerified": true,
    "enabled": true
  }'
```

**Response (200 OK):**
```json
{
  "id": "e0f94514-b71a-4c06-beaf-4c764555e9f9",
  "username": "john_doe_updated",
  "email": "john.updated@example.com",
  "enabled": true
}
```

**Error Responses:**
- `400` - Invalid input
- `404` - User not found
- `500` - Keycloak unavailable

---

## 6. Reset User Password

Resets the password for a Keycloak user.

```bash
curl -X PUT http://localhost:5000/api/keycloak/users/{keycloak-id}/password \
  -H "Content-Type: application/json" \
  -d '{
    "password": "NewSecurePass456!"
  }'
```

**Response:** `204 No Content`

**Error Responses:**
- `400` - Password missing
- `404` - User not found
- `500` - Keycloak unavailable

---

## 7. Assign Roles to User

Assigns client roles to a Keycloak user.

```bash
curl -X PUT http://localhost:5000/api/keycloak/users/{keycloak-id}/roles \
  -H "Content-Type: application/json" \
  -d '{
    "roles": ["ADMIN", "MANAGER"]
  }'
```

**Response:** `204 No Content`

**Error Responses:**
- `400` - Roles list missing or invalid
- `404` - User not found
- `500` - Keycloak unavailable

---

## 8. Get User Roles

Returns client roles assigned to a Keycloak user.

```bash
curl -X GET http://localhost:5000/api/keycloak/users/{keycloak-id}/roles
```

**Response (200 OK):**
```json
{
  "userId": "e0f94514-b71a-4c06-beaf-4c764555e9f9",
  "roles": ["USER", "ADMIN"]
}
```

---

## 9. List Available Roles

Returns all available client roles for sales-client.

```bash
curl -X GET http://localhost:5000/api/keycloak/users/roles
```

**Response (200 OK):**
```json
{
  "roles": ["ADMIN", "MANAGER", "CASHIER", "USER", "CUSTOMER"]
}
```

---

## 10. Delete User

Permanently deletes a user from Keycloak and local database.

```bash
curl -X DELETE http://localhost:5000/api/keycloak/users/{keycloak-id}
```

**Response:** `204 No Content`

**Error Responses:**
- `404` - User not found
- `500` - Keycloak unavailable

---

## Quick Reference

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/keycloak/users` | Create user |
| `GET` | `/api/keycloak/users` | List all users |
| `GET` | `/api/keycloak/users/{id}` | Get user by Keycloak ID |
| `GET` | `/api/keycloak/users/username/{username}` | Get user by username |
| `PUT` | `/api/keycloak/users/{id}` | Update user |
| `PUT` | `/api/keycloak/users/{id}/password` | Reset password |
| `PUT` | `/api/keycloak/users/{id}/roles` | Assign roles |
| `GET` | `/api/keycloak/users/{id}/roles` | Get user roles |
| `GET` | `/api/keycloak/users/roles` | List available roles |
| `DELETE` | `/api/keycloak/users/{id}` | Delete user |
