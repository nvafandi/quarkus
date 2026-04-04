#!/bin/bash

# Simple test script for password encryption
# Run this AFTER starting the server manually

BASE_URL="http://localhost:8080/api"

echo "========================================="
echo "  Testing Password Encryption"
echo "========================================="
echo ""
echo "⚠️  Make sure the server is running first!"
echo "   Run in another terminal: ./mvnw quarkus:dev"
echo ""

read -p "Is the server running? (y/n): " answer
if [ "$answer" != "y" ]; then
    echo "Please start the server first, then run this script."
    exit 1
fi

echo ""
echo "Creating test user..."
echo ""

# Create a test user
curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpass123",
    "role": "CUSTOMER"
  }' | python3 -m json.tool 2>/dev/null || curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpass123",
    "role": "CUSTOMER"
  }'

echo ""
echo ""
echo "========================================="
echo "  To verify encryption in database:"
echo "========================================="
echo ""
echo "Run this command (you'll need sudo for psql):"
echo ""
echo "  sudo -u postgres psql -d sales_db -c \"SELECT username, LEFT(password, 40) as hash FROM users;\""
echo ""
echo "You should see BCrypt hashes starting with: \$2a\$12\$"
echo "NOT plain text passwords!"
echo "========================================="
