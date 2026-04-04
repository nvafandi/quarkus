# Password Encryption - Bug Fix

## 🐛 Issue Found and Fixed

**Problem:** The `@NotBlank` validation constraint on the password field was causing errors when:
1. Updating a user without changing the password
2. Sending empty/null password during updates

**Root Cause:** The `@NotBlank` validation was applied to all operations (create AND update), but during updates, you might not want to change the password.

## ✅ What Was Fixed

### 1. Removed `@NotBlank` from Password Field

**Before:**
```java
@NotBlank(message = "Password is required")
private String password;
```

**After:**
```java
@Size(max = 100, message = "Password must not exceed 100 characters")
private String password;
```

### 2. Added Validation in Service Layer

**Updated `UserService.create()` method:**
```java
@Transactional
public UserDTO create(UserDTO userDTO) {
    // Validate password is provided during creation
    if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
        throw new WebApplicationException("Password is required", Response.Status.BAD_REQUEST);
    }
    
    // ... rest of the creation logic
}
```

**Updated `UserService.update()` method:**
```java
@Transactional
public UserDTO update(UUID id, UserDTO userDTO) {
    UserEntity entity = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    
    entity.setUsername(userDTO.getUsername());
    
    // Only encrypt if new password is provided
    if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
        entity.setPassword(PasswordEncoder.encode(userDTO.getPassword()));
    }
    
    entity.setRole(userDTO.getRole());
    return toDTO(entity);
}
```

## 🎯 How It Works Now

### Creating a User (Password REQUIRED)

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "secret123",
    "role": "CUSTOMER"
  }'
```

**✅ Success (201 Created):**
```json
{
  "id": "019d...",
  "username": "john_doe",
  "password": null,
  "role": "CUSTOMER",
  "createdAt": "...",
  "updatedAt": "..."
}
```

**❌ Error (400 Bad Request) - Missing Password:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "",
    "role": "CUSTOMER"
  }'
```

Response:
```json
{
  "status": 400,
  "message": "Password is required",
  "timestamp": "2026-04-04T..."
}
```

### Updating a User (Password OPTIONAL)

**Option 1: Update WITH new password**
```bash
curl -X PUT http://localhost:8080/api/users/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe_updated",
    "password": "newpassword123",
    "role": "ADMIN"
  }'
```
✅ Password will be re-encrypted

**Option 2: Update WITHOUT changing password**
```bash
curl -X PUT http://localhost:8080/api/users/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe_updated",
    "role": "ADMIN"
  }'
```
✅ Existing password remains unchanged

## 📋 Validation Rules

| Operation | Password Required? | Behavior |
|-----------|-------------------|----------|
| **CREATE** | ✅ Yes | Must provide password, will be encrypted |
| **UPDATE** | ❌ No | If provided, will be encrypted. If empty/null, keeps existing password |

## 🧪 Testing

### Test 1: Create User with Password
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","role":"ADMIN"}'
```
Expected: `201 Created` with encrypted password

### Test 2: Create User without Password (Should Fail)
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"","role":"CUSTOMER"}'
```
Expected: `400 Bad Request` - "Password is required"

### Test 3: Update User with New Password
```bash
curl -X PUT http://localhost:8080/api/users/{id} \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"newpass123","role":"ADMIN"}'
```
Expected: `200 OK` - Password re-encrypted

### Test 4: Update User without Password
```bash
curl -X PUT http://localhost:8080/api/users/{id} \
  -H "Content-Type: application/json" \
  -d '{"username":"admin_updated","role":"ADMIN"}'
```
Expected: `200 OK` - Existing password preserved

## ✅ Build Status

```
[INFO] BUILD SUCCESS
```

All changes compile successfully! 🎉

## 🔒 Security Features (Still Working)

- ✅ BCrypt encryption with 12 iterations
- ✅ Automatic salt generation
- ✅ Password encrypted on create
- ✅ Password re-encrypted on update (if provided)
- ✅ Password never returned in API responses
- ✅ Validation ensures password on create
- ✅ Flexible updates (password optional)

---

**Status:** ✅ Bug fixed and ready to test!
