#!/bin/bash

# Script to stop all microservices
set -e

echo "Stopping Java Microservices Sandbox..."

# Navigate to infrastructure directory
cd "$(dirname "$0")/../infrastructure" || exit 1

# Stop services
if command -v docker-compose &> /dev/null; then
    docker-compose down
else
    docker compose down
fi

echo "Services stopped successfully!"