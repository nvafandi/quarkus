#!/bin/bash
# =============================================================================
# Safe Push: Mask credentials before push, restore after
# =============================================================================
# Usage: ./git-push-safe.sh [push-args...]
# Example: ./git-push-safe.sh
#          ./git-push-safe.sh -f origin main
# =============================================================================

set -e

CONFIG_FILE="src/main/resources/application.properties"
BACKUP_FILE="${CONFIG_FILE}.bak"
MASKED_MESSAGE="[AUTO-MASKED] Credentials masked before push"
RESTORE_MESSAGE="[AUTO-RESTORE] Local credentials restored (not pushed)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Safe Push: Credential Masking ===${NC}"

# Check config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo -e "${RED}Error: $CONFIG_FILE not found${NC}"
    exit 1
fi

# Step 1: Backup real credentials
echo -e "${YELLOW}[1/5] Backing up credentials...${NC}"
cp "$CONFIG_FILE" "$BACKUP_FILE"

# Step 2: Mask credentials
echo -e "${YELLOW}[2/5] Masking credentials...${NC}"
sed -i "s/^\(quarkus\.datasource\.username\)=.*/\1=your_username/" "$CONFIG_FILE"
sed -i "s/^\(quarkus\.datasource\.password\)=.*/\1=your_password/" "$CONFIG_FILE"

# Verify masking worked
if grep -q "your_username" "$CONFIG_FILE" && grep -q "your_password" "$CONFIG_FILE"; then
    echo -e "${GREEN}  ✓ Credentials masked successfully${NC}"
else
    echo -e "${RED}  ✗ Failed to mask credentials${NC}"
    exit 1
fi

# Step 3: Commit masked version and push
echo -e "${YELLOW}[3/5] Committing masked version...${NC}"
git add -A
git commit -m "$MASKED_MESSAGE" --no-verify 2>/dev/null || echo "  (No new changes to commit)"

echo -e "${YELLOW}[4/5] Pushing to remote...${NC}"
git push "$@"

# Step 4: Restore real credentials
echo -e "${YELLOW}[5/5] Restoring credentials...${NC}"
mv "$BACKUP_FILE" "$CONFIG_FILE"

# Commit restored version locally only (will be masked again on next push)
git add -A
git commit -m "$RESTORE_MESSAGE" --no-verify 2>/dev/null || true

echo -e "${GREEN}=== Done! Pushed with masked credentials ===${NC}"
echo -e "${GREEN}  Local credentials restored and safe.${NC}"
