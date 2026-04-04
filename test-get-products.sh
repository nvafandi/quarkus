#!/bin/bash

# Test GET /api/products endpoint
# Verifies product data exists and displays summary

API_URL="http://localhost:5000/api/products"

echo "=========================================="
echo " Testing: GET /api/products"
echo "=========================================="
echo ""

# Make GET request
echo "Request:"
echo "GET $API_URL"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL" \
  -H "Content-Type: application/json" 2>/dev/null)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "Response:"
echo "HTTP Status: $HTTP_CODE"
echo ""

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Success! Products retrieved."
    echo ""
    
    # Count products
    PRODUCT_COUNT=$(echo "$BODY" | python3 -c "import sys,json; data=json.load(sys.stdin); print(len(data.get('data', [])))" 2>/dev/null)
    
    if [ -n "$PRODUCT_COUNT" ] && [ "$PRODUCT_COUNT" != "0" ]; then
        echo "📊 Total Products: $PRODUCT_COUNT"
        echo ""
        
        # Display first 5 products
        echo "Sample Products (First 5):"
        echo "------------------------------------------"
        echo "$BODY" | python3 -c "
import sys, json
data = json.load(sys.stdin)
products = data.get('data', [])[:5]
for i, p in enumerate(products, 1):
    print(f\"{i}. ID: {p.get('id')}\")
    print(f\"   Name: {p.get('name')}\")
    print(f\"   Price: Rp {float(p.get('price', 0)):,.2f}\")
    print(f\"   Stock: {p.get('stock')} units\")
    print(f\"   Created By: {p.get('createdBy') or 'N/A'}\")
    print(f\"   Updated By: {p.get('updatedBy') or 'N/A'}\")
    print(f\"   Created At: {p.get('createdAt')}\")
    print()
" 2>/dev/null
        
        # Price statistics
        echo "=========================================="
        echo " Price Statistics"
        echo "=========================================="
        echo "$BODY" | python3 -c "
import sys, json
data = json.load(sys.stdin)
products = data.get('data', [])
prices = [float(p.get('price', 0)) for p in products]
if prices:
    print(f\"  Min Price: Rp {min(prices):,.2f}\")
    print(f\"  Max Price: Rp {max(prices):,.2f}\")
    print(f\"  Avg Price: Rp {sum(prices)/len(prices):,.2f}\")
    print(f\"  Total Stock: {sum(p.get('stock', 0) for p in products):,} units\")
" 2>/dev/null
        
    else
        echo "⚠️  No products found in database."
        echo ""
        echo "To insert products, run:"
        echo "  psql -U nurvan -d sales_db -f insert-1000-products.sql"
    fi
elif [ "$HTTP_CODE" = "000" ]; then
    echo "❌ Connection failed!"
    echo "   Is the server running on port 5000?"
    echo ""
    echo "   Start server: ./mvnw quarkus:dev"
else
    echo "❌ Error! HTTP $HTTP_CODE"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
fi

echo ""
echo "=========================================="
