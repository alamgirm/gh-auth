#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    echo "Loading environment variables from .env..."
    export $(grep -v '^#' .env | grep -v '^export' | xargs)
    source <(grep '^export' .env)
fi

# Verify critical variables are set
echo "Checking configuration..."
echo "GITHUB_CLIENT_ID: ${GITHUB_CLIENT_ID:-(not set)}"
echo "BE_CLIENT_ID: ${BE_CLIENT_ID:-(not set)}"
echo "BE_TENANT_ID: ${BE_TENANT_ID:-(not set)}"

if [ -z "$GITHUB_CLIENT_ID" ]; then
    echo "⚠️  WARNING: GITHUB_CLIENT_ID is not set"
fi

if [ -z "$BE_CLIENT_ID" ]; then
    echo "⚠️  WARNING: BE_CLIENT_ID is not set"
fi

echo ""
echo "Starting backend..."
./gradlew bootRun

