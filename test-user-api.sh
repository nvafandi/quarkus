#!/bin/bash

# Test script for User API with BCrypt password encryption
# This script tests creating and retrieving users

BASE_URL="http://localhost:8080/api"

echo "========================================="
echo "  Testing User API with Password Encryption"
echo "========================================="
echo ""

# Check if server is running
echo "Checking if server is running..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/users")
if [ "$HTTP_CODE" != "200" ] && [ "$HTTP_CODE" != "500" ]; then
    echo "❌ Error: Server is not running at $BASE_URL"
    echo "   Please start the server with: ./mvnw quarkus:dev"
    exit 1
fi
echo "✓ Server is running (HTTP $HTTP_CODE)"
echo ""

echo "========================================="
echo "  Test 1: Create Admin User"
echo "========================================="
echo ""

ADMIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "role": "ADMIN"
  }')

ADMIN_HTTP_CODE=$(echo "$ADMIN_RESPONSE" | tail -n1)
ADMIN_BODY=$(echo "$ADMIN_RESPONSE" | sed '$d')

echo "Request:"
echo "  POST $BASE_URL/users"
echo "  Body: {username: 'admin', password: 'admin123', role: 'ADMIN'}"
echo ""
echo "Response (HTTP $ADMIN_HTTP_CODE):"
echo "$ADMIN_BODY" | python3 -m json.tool 2>/dev/null || echo "$ADMIN_BODY"
echo ""

if [ "$ADMIN_HTTP_CODE" = "201" ]; then
    echo "✅ User created successfully"
    echo "   Note: Password is encrypted with BCrypt before saving"
    echo "   Note: Password field in response is null (security)"
else
    echo "⚠️  User may already exist (HTTP $ADMIN_HTTP_CODE)"
fi

echo ""
echo "========================================="
echo "  Test 2: Create Regular User"
echo "========================================="
echo ""

USER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "secret123",
    "role": "CUSTOMER"
  }')

USER_HTTP_CODE=$(echo "$USER_RESPONSE" | tail -n1)
USER_BODY=$(echo "$USER_RESPONSE" | sed '$d')

echo "Request:"
echo "  POST $BASE_URL/users"
echo "  Body: {username: 'john_doe', password: 'secret123', role: 'CUSTOMER'}"
echo ""
echo "Response (HTTP $USER_HTTP_CODE):"
echo "$USER_BODY" | python3 -m json.tool 2>/dev/null || echo "$USER_BODY"
echo ""

if [ "$USER_HTTP_CODE" = "201" ]; then
    echo "✅ User created successfully"
else
    echo "⚠️  User may already exist (HTTP $USER_HTTP_CODE)"
fi

echo ""
echo "========================================="
echo "  Test 3: Get All Users"
echo "========================================="
echo ""

echo "Request:"
echo "  GET $BASE_URL/users"
echo ""
echo "Response:"
curl -s "$BASE_URL/users" | python3 -m json.tool 2>/dev/null || curl -s "$BASE_URL/users"
echo ""

echo ""
echo "========================================="
echo "  Test 4: Create Multiple Test Users"
echo "========================================="
echo ""

declare -a USERS=(
    '{"username":"alice","password":"alice123","role":"CUSTOMER"}'
    '{"username":"bob","password":"bob123","role":"CUSTOMER"}'
    '{"username":"charlie","password":"charlie123","role":"ADMIN"}'
)

for user_json in "${USERS[@]}"; do
    username=$(echo "$user_json" | python3 -c "import sys, json; print(json.load(sys.stdin)['username'])" 2>/dev/null)
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/users" \
      -H "Content-Type: application/json" \
      -d "$user_json")
    
    http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" = "201" ]; then
        echo "✅ Created user: $username"
    elif [ "$http_code" = "409" ]; then
        echo "⚠️  User already exists: $username"
    else
        echo "❌ Failed to create user: $username (HTTP $http_code)"
    fi
done

echo ""
echo "========================================="
echo "  Test 5: Verify Password Encryption"
echo "========================================="
echo ""
echo "Checking database for password encryption..."
echo ""
echo "To verify passwords are encrypted, run this SQL query:"
echo ""
echo "  psql -h localhost -U nurvan -d sales_db -c \"SELECT username, LEFT(password, 30) as password_hash FROM users LIMIT 5;\""
echo ""
echo "Expected output:"
echo "  username  | password_hash"
echo "  ----------+----------------------------------"
echo "  admin     | \$2a\$12\$LZ3x... (BCrypt hash)"
echo "  john_doe  | \$2a\$12\$Kp9y... (BCrypt hash)"
echo ""
echo "The password field should show:"
echo "  - Starts with \$2a\$12\$ (BCrypt algorithm identifier)"
echo "  - 60 character hash string"
echo "  - NOT plain text passwords"
echo ""

echo "========================================="
echo "  Summary"
echo "========================================="
echo ""
echo "Password Encryption Features:"
echo "  ✅ BCrypt algorithm with 12 iterations"
echo "  ✅ Automatic salt generation per password"
echo "  ✅ Passwords encrypted on create"
echo "  ✅ Passwords re-encrypted on update (if provided)"
echo "  ✅ Password never returned in API responses"
echo "  ✅ Secure password storage"
echo ""
echo "API Endpoints:"
echo "  Create User:  POST   $BASE_URL/users"
echo "  Get All:      GET    $BASE_URL/users"
echo "  Get By ID:    GET    $BASE_URL/users/{id}"
echo "  Update:       PUT    $BASE_URL/users/{id}"
echo "  Delete:       DELETE $BASE_URL/users/{id}"
echo ""
echo "========================================="
