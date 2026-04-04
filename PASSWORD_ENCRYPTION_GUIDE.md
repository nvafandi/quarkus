# Password Encryption - Testing Guide

## ✅ Changes Implemented

Password encryption has been successfully added to the User API using **BCrypt** algorithm.

### What Changed

1. **Added Dependency** - `quarkus-elytron-security` in `pom.xml`
2. **Created PasswordEncoder** - Utility class using Quarkus `BcryptUtil`
3. **Updated UserService** - Automatically encrypts passwords on create/update
4. **Updated UserDTO** - Password field marked as writeOnly, never returned in responses

### Files Modified

- `pom.xml` - Added security dependency
- `src/main/java/com/sales/util/PasswordEncoder.java` - New utility class
- `src/main/java/com/sales/service/UserService.java` - Added encryption logic
- `src/main/java/com/sales/dto/UserDTO.java` - Updated password field documentation

## 🧪 How to Test

### Step 1: Ensure Database is Running

```bash
# Check PostgreSQL is running
# (depends on your OS setup)

# Make sure database 'sales_db' exists
# and credentials in application.properties are correct
```

### Step 2: Start the Server

```bash
cd /home/nurvan/Project/sales-system
./mvnw quarkus:dev
```

Wait for: `Listening on: http://localhost:8080`

### Step 3: Test Creating Users

**Option 1: Using the test script**
```bash
./test-user-api.sh
```

**Option 2: Manual curl commands**

```bash
# Create an admin user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "role": "ADMIN"
  }'

# Expected response (password is null):
{
  "id": "019d...",
  "username": "admin",
  "password": null,
  "role": "ADMIN",
  "createdAt": "2026-04-04T...",
  "updatedAt": "2026-04-04T..."
}
```

```bash
# Create a regular user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "secret123",
    "role": "CUSTOMER"
  }'
```

### Step 4: Verify Encryption in Database

If you have `psql` client installed:

```bash
psql -h localhost -U nurvan -d sales_db -c \
  "SELECT username, LEFT(password, 30) as password_hash FROM users LIMIT 5;"
```

**Expected Output:**
```
 username  |        password_hash        
-----------+-----------------------------
 admin     | $2a$12$LZ3x...
 john_doe  | $2a$12$Kp9y...
```

**NOT plain text:**
```
 username  | password   ❌ WRONG
-----------+----------
 admin     | admin123
```

### Step 5: Get Users (Password Should Be Null)

```bash
curl http://localhost:8080/api/users | jq '.'
```

**Expected Response:**
```json
[
  {
    "id": "019d...",
    "username": "admin",
    "password": null,        // ← Always null, never returned
    "role": "ADMIN",
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```

## 🔐 Security Features

| Feature | Status | Details |
|---------|--------|---------|
| **BCrypt Algorithm** | ✅ | Industry standard password hashing |
| **Salt Generation** | ✅ | Unique random salt per password |
| **Iterations** | ✅ | 12 iterations (strong security) |
| **Auto-encrypt on Create** | ✅ | Transparent to API client |
| **Re-encrypt on Update** | ✅ | Only if new password provided |
| **Never Expose Hash** | ✅ | Password field always null in responses |
| **Write-only Field** | ✅ | OpenAPI marks as writeOnly |

## 📝 API Usage Examples

### Create User
```bash
POST /api/users
Content-Type: application/json

{
  "username": "alice",
  "password": "alice123",    ← Plain text (will be encrypted)
  "role": "CUSTOMER"
}
```

### Response
```json
{
  "id": "019d...",
  "username": "alice",
  "password": null,           ← Never returned
  "role": "CUSTOMER",
  "createdAt": "...",
  "updatedAt": "..."
}
```

### Update User (with new password)
```bash
PUT /api/users/{id}
Content-Type: application/json

{
  "username": "alice",
  "password": "newpassword",  ← Will be re-encrypted
  "role": "ADMIN"
}
```

### Update User (keep existing password)
```bash
PUT /api/users/{id}
Content-Type: application/json

{
  "username": "alice_updated",
  "password": "",             ← Empty = keep existing
  "role": "ADMIN"
}
```

## 🔍 How BCrypt Works

```
Input:  "secret123"
         ↓
    BCrypt Hash (12 iterations, random salt)
         ↓
Output: "$2a$12$LZ3xK9mP2qR4sT6uV8wX0y.A1bC3dE5fG7hI9jK1lM3nO5pQ7rS9t"
         ↑         ↑
    Algorithm  Cost factor
    identifier (2^12 = 4096 rounds)
```

**Key Points:**
- Each hash is unique (random salt)
- Same password → different hashes
- Computationally expensive (prevents brute force)
- One-way function (cannot reverse)

## 🧪 Unit Test Example

```java
import com.sales.util.PasswordEncoder;

// Test password encoding
String rawPassword = "secret123";
String encoded = PasswordEncoder.encode(rawPassword);

// Verify it's encrypted
assert encoded.startsWith("$2a$12$");
assert encoded.length() == 60;

// Verify matching
assert PasswordEncoder.matches(rawPassword, encoded);
assert !PasswordEncoder.matches("wrongpassword", encoded);
```

## ⚠️ Important Notes

1. **Existing Users**: Users created before this change have plain text passwords. They need to be recreated or have their passwords updated.

2. **Password Policy**: The API doesn't enforce password complexity. Consider adding validation rules if needed.

3. **Authentication**: This implementation handles password storage. For login/authentication, you'll need to implement a login endpoint that verifies passwords using `PasswordEncoder.matches()`.

4. **Database Migration**: If you have existing users with plain text passwords, create a migration script to re-encrypt them:

```java
// One-time migration script
List<UserEntity> users = userRepository.listAll();
for (UserEntity user : users) {
    if (!user.getPassword().startsWith("$2a$")) {
        user.setPassword(PasswordEncoder.encode(user.getPassword()));
    }
}
```

## 📚 References

- [Quarkus Security Guide](https://quarkus.io/guides/security)
- [BCrypt Documentation](https://en.wikipedia.org/wiki/Bcrypt)
- [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)

---

**Status:** ✅ Password encryption is fully implemented and ready to use!
