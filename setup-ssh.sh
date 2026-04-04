#!/bin/bash

# SSH Server Setup Script
# Run this script with: sudo bash setup-ssh.sh

echo "========================================="
echo "  SSH Server Setup"
echo "========================================="
echo ""

# Step 1: Install OpenSSH Server
echo "Step 1: Installing OpenSSH Server..."
apt update -y
apt install -y openssh-server

if [ $? -eq 0 ]; then
    echo "✅ OpenSSH Server installed successfully"
else
    echo "❌ Failed to install OpenSSH Server"
    exit 1
fi
echo ""

# Step 2: Start and Enable SSH Service
echo "Step 2: Starting SSH Service..."
systemctl start ssh
systemctl enable ssh
systemctl status ssh --no-pager -l

if systemctl is-active --quiet ssh; then
    echo ""
    echo "✅ SSH Service is running"
else
    echo ""
    echo "❌ SSH Service failed to start"
    exit 1
fi
echo ""

# Step 3: Configure Firewall
echo "Step 3: Configuring Firewall..."
if command -v ufw &> /dev/null; then
    ufw allow ssh
    ufw reload
    echo "✅ Firewall configured for SSH"
else
    echo "⚠️  UFW not installed, skipping firewall config"
fi
echo ""

# Step 4: Get IP Address
echo "Step 4: Your Network Information"
echo "========================================="
echo ""

# Get IP addresses
echo "Local IP Addresses:"
ip -4 addr show | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v 127.0.0.1 | while read ip; do
    echo "  - $ip"
done
echo ""

# Get public IP
echo "Public IP (for internet access):"
PUBLIC_IP=$(curl -s ifconfig.me 2>/dev/null || echo "Could not determine")
echo "  - $PUBLIC_IP"
echo ""

echo "========================================="
echo "  SSH Connection Details"
echo "========================================="
echo ""
echo "To connect from another computer:"
echo ""
echo "  Local Network:"
echo "    ssh nurvan@<YOUR_LOCAL_IP>"
echo ""
echo "  From Internet (requires port forwarding):"
echo "    ssh nurvan@<YOUR_PUBLIC_IP>"
echo ""

# Step 5: SSH Key Setup (Optional)
echo "========================================="
echo "  SSH Key Authentication (Optional)"
echo "========================================="
echo ""
echo "For password-less login, run on your OTHER computer:"
echo ""
echo "  ssh-keygen -t ed25519"
echo "  ssh-copy-id nurvan@<YOUR_IP>"
echo ""

echo "========================================="
echo "  Quick Commands"
echo "========================================="
echo ""
echo "  Check SSH status:    sudo systemctl status ssh"
echo "  Start SSH:           sudo systemctl start ssh"
echo "  Stop SSH:            sudo systemctl stop ssh"
echo "  Restart SSH:         sudo systemctl restart ssh"
echo "  View logs:           sudo journalctl -u ssh -f"
echo ""
echo "========================================="
echo "  ✅ SSH Setup Complete!"
echo "========================================="
echo ""

read -p "Would you like to test SSH connection? (y/n): " answer
if [ "$answer" = "y" ]; then
    echo ""
    echo "Testing SSH connection to localhost..."
    ssh -o BatchMode=yes -o ConnectTimeout=5 localhost echo "✅ SSH is working!" 2>&1 || echo "⚠️  SSH test failed, but setup is complete"
fi
