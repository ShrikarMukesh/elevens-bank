#!/bin/bash

VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="root"

echo "Using Vault at $VAULT_ADDR with token $VAULT_TOKEN"

# 1. Enable Transit Engine
echo "Enabling Transit Engine..."
curl -X POST \
    -H "X-Vault-Token: $VAULT_TOKEN" \
    -d '{"type": "transit"}' \
    $VAULT_ADDR/v1/sys/mounts/transit

# 2. Create Key
echo -e "\nCreating Key 'kyc-pii'..."
curl -X POST \
    -H "X-Vault-Token: $VAULT_TOKEN" \
    -d '{"type": "aes256-gcm96"}' \
    $VAULT_ADDR/v1/transit/keys/kyc-pii

echo -e "\nDone! Verification:"
curl -H "X-Vault-Token: $VAULT_TOKEN" $VAULT_ADDR/v1/transit/keys/kyc-pii
