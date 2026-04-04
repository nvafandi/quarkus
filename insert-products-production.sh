#!/bin/bash

# Insert 1000 products into PRODUCTION database via API
# Usage: ./insert-products-production.sh
# Note: Requires server to be running and authenticated

API_URL="http://localhost:5000/api/products"
SUCCESS_COUNT=0
FAIL_COUNT=0
TOKEN=""

echo "=========================================="
echo " Inserting 1000 Products (PRODUCTION)"
echo "=========================================="
echo ""

# First, get authentication token from Keycloak
echo "Step 1: Authenticating with Keycloak..."
read -p "Enter Keycloak username: " KC_USERNAME
read -sp "Enter Keycloak password: " KC_PASSWORD
echo ""

TOKEN_RESPONSE=$(curl -s -X POST http://localhost:8180/realms/sales-realm/protocol/openid-connect/token \
  -d "client_id=sales-client" \
  -d "client_secret=oqcZx7sKP0CW2NV2yPpN6YiXGit8CtT6" \
  -d "grant_type=password" \
  -d "username=$KC_USERNAME" \
  -d "password=$KC_PASSWORD" 2>/dev/null)

TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
    echo "❌ Authentication failed. Please check credentials."
    exit 1
fi

echo "✅ Authentication successful!"
echo ""
echo "Step 2: Inserting products..."
echo ""

# Product name components
NAMES=("Laptop" "Phone" "Tablet" "Monitor" "Keyboard" "Mouse" "Headphones" "Speaker" "Camera" "Printer" "Scanner" "Router" "SSD" "RAM" "CPU" "GPU" "Motherboard" "Power Supply" "Case" "Cooling Fan" "Webcam" "Microphone" "Projector" "UPS" "Cable" "Adapter" "Battery" "Charger" "Smartwatch" "Earbuds" "Switch" "Hub" "Dock" "Stand" "Backpack" "Bag" "Sleeve" "Screen Protector" "Cleaning Kit")
PREFIXES=("Pro" "Plus" "Max" "Ultra" "Lite" "Air" "Mini" "Elite")

for i in $(seq 1 1000); do
    # Generate random product data
    NAME=${NAMES[$((RANDOM % ${#NAMES[@]}))]}
    PREFIX=${PREFIXES[$((RANDOM % ${#PREFIXES[@]}))]}
    MODEL=$((RANDOM % 900 + 100))
    FULL_NAME="${NAME} ${PREFIX} ${MODEL} - ${i}"
    
    # Random price (100000 to 10000000)
    PRICE=$((RANDOM % 9900000 + 100000))
    PRICE_FORMATTED=$(printf "%.2f" "$PRICE")
    
    # Random stock (10 to 500)
    STOCK=$((RANDOM % 491 + 10))
    
    # Create JSON body
    JSON_BODY="{
      \"name\": \"${FULL_NAME}\",
      \"price\": ${PRICE_FORMATTED},
      \"stock\": ${STOCK}
    }"
    
    # Make API call with authentication
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d "$JSON_BODY" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    
    if [ "$HTTP_CODE" = "201" ]; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        if [ $((i % 100)) -eq 0 ] || [ $i -le 10 ]; then
            echo "[$i/1000] ✅ Created: ${FULL_NAME}"
        fi
    elif [ "$HTTP_CODE" = "000" ]; then
        echo "[$i/1000] ❌ Connection failed - is server running on port 5000?"
        break
    else
        FAIL_COUNT=$((FAIL_COUNT + 1))
        if [ $((i % 100)) -eq 0 ]; then
            echo "[$i/1000] ❌ Failed (HTTP $HTTP_CODE)"
        fi
    fi
    
    # Small delay to avoid overwhelming the server
    sleep 0.05
done

echo ""
echo "=========================================="
echo " Summary"
echo "=========================================="
echo "✅ Success: $SUCCESS_COUNT"
echo "❌ Failed:   $FAIL_COUNT"
echo "📊 Total:   1000"
echo "=========================================="
echo ""
echo "Data inserted into PostgreSQL database!"
echo "Connect to verify: psql -U nurvan -d sales_db -c 'SELECT COUNT(*) FROM products;'"
