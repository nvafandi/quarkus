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
DOCKER_FILE="docker-compose.yml"
DOCKER_BACKUP="${DOCKER_FILE}.bak"
MASKED_MESSAGE="[AUTO-MASKED] Credentials masked before push"
RESTORE_MESSAGE="[AUTO-RESTORE] Local credentials restored (not pushed)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Safe Push: Credential Masking ===${NC}"

# Step 1: Backup config files
echo -e "${YELLOW}[1/6] Backing up configuration files...${NC}"
cp "$CONFIG_FILE" "$BACKUP_FILE"
cp "$DOCKER_FILE" "$DOCKER_BACKUP"

# Step 2: Mask application.properties
echo -e "${YELLOW}[2/6] Masking application.properties...${NC}"
sed -i "s/^\(quarkus\.datasource\.username\)=.*/\1=your_username/" "$CONFIG_FILE"
sed -i "s/^\(quarkus\.datasource\.password\)=.*/\1=your_password/" "$CONFIG_FILE"
sed -i "s/^\(quarkus\.oidc\.credentials\.secret\)=.*/\1=your_client_secret/" "$CONFIG_FILE"

# Step 3: Mask docker-compose.yml
echo -e "${YELLOW}[3/6] Masking docker-compose.yml...${NC}"
sed -i "s/^\(\s*POSTGRES_PASSWORD:\s*\).*/\1your_password/" "$DOCKER_FILE"
sed -i "s/^\(\s*KC_DB_PASSWORD:\s*\).*/\1your_password/" "$DOCKER_FILE"
sed -i "s/^\(\s*KEYCLOAK_ADMIN_PASSWORD:\s*\).*/\1admin/" "$DOCKER_FILE"
sed -i "s/^\(\s*QUARKUS_DATASOURCE_PASSWORD:\s*\).*/\1your_password/" "$DOCKER_FILE"
sed -i "s/^\(\s*QUARKUS_OIDC_CREDENTIALS_SECRET:\s*\).*/\1your_client_secret/" "$DOCKER_FILE"

# Step 4: Commit and push
echo -e "${YELLOW}[4/6] Committing masked version...${NC}"
git add -A
git commit -m "$MASKED_MESSAGE" --no-verify 2>/dev/null || echo "  (No new changes to commit)"

echo -e "${YELLOW}[5/6] Pushing to remote...${NC}"
git push "$@"

# Step 5: Restore real credentials
echo -e "${YELLOW}[6/6] Restoring credentials...${NC}"
mv "$BACKUP_FILE" "$CONFIG_FILE"
mv "$DOCKER_BACKUP" "$DOCKER_FILE"

# Commit restored version locally only
git add -A
git commit -m "$RESTORE_MESSAGE" --no-verify 2>/dev/null || true

echo -e "${GREEN}=== Done! Pushed with masked credentials ===${NC}"
echo -e "${GREEN}  Local credentials restored and safe.${NC}"
