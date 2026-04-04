#!/bin/bash

# Generate 100 dummy users via API
# Usage: ./insert-100-users.sh

API_URL="http://localhost:5000/api/keycloak/users"
SUCCESS_COUNT=0
FAIL_COUNT=0

echo "=========================================="
echo " Inserting 100 Dummy Users via API"
echo "=========================================="
echo ""

ROLES=("USER" "USER" "USER" "CASHIER" "MANAGER" "USER" "USER" "USER")
DOMAINS=("example.com" "test.org" "demo.net" "sample.io")

for i in $(seq 1 100); do
    # Generate random data
    FIRSTNAME=("john" "jane" "alice" "bob" "charlie" "diana" "edward" "fiona" "george" "helen" "ivan" "julia" "kevin" "laura" "mike" "nina" "oscar" "paula" "quinn" "rachel")
    LASTNAME=("smith" "doe" "brown" "wilson" "taylor" "davis" "miller" "anderson" "thomas" "jackson")

    FNAME=${FIRSTNAME[$((RANDOM % ${#FIRSTNAME[@]}))]}
    LNAME=${LASTNAME[$((RANDOM % ${#LASTNAME[@]}))]}
    USERNAME="${FNAME}_${LNAME}_${i}_$(date +%s)"
    EMAIL="${USERNAME}@${DOMAINS[$((RANDOM % ${#DOMAINS[@]}))]}"
    ROLE=${ROLES[$((RANDOM % ${#ROLES[@]}))]}

    # Create JSON body
    JSON_BODY="{
      \"username\": \"$USERNAME\",
      \"password\": \"TestPass123!\",
      \"email\": \"$EMAIL\",
      \"emailVerified\": true,
      \"roles\": [\"$ROLE\"]
    }"

    # Make API call
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL" \
      -H "Content-Type: application/json" \
      -d "$JSON_BODY")

    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "201" ]; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        echo "[$i/100] ✅ Created: $USERNAME (Role: $ROLE)"
    elif [ "$HTTP_CODE" = "409" ]; then
        FAIL_COUNT=$((FAIL_COUNT + 1))
        echo "[$i/100] ⚠️  Duplicate: $USERNAME"
    elif [ "$HTTP_CODE" = "000" ]; then
        FAIL_COUNT=$((FAIL_COUNT + 1))
        echo "[$i/100] ❌ Connection failed - is server running?"
        break
    else
        FAIL_COUNT=$((FAIL_COUNT + 1))
        echo "[$i/100] ❌ Failed ($HTTP_CODE): $USERNAME"
    fi

    # Small delay to avoid overwhelming the server
    sleep 0.1
done

echo ""
echo "=========================================="
echo " Summary"
echo "=========================================="
echo "✅ Success: $SUCCESS_COUNT"
echo "❌ Failed:   $FAIL_COUNT"
echo "📊 Total:   100"
echo "=========================================="
