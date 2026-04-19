#!/bin/bash

# Script to start all microservices with Docker Compose
set -e

echo "Starting Java Microservices Sandbox..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "Error: Docker Compose is not installed. Please install Docker Compose."
    exit 1
fi

# Check if .env file exists, copy from example if not
if [ ! -f .env ]; then
    echo "Warning: .env file not found. Creating from .env.example..."
    cp .env.example .env
    echo "Please update the .env file with your configuration."
fi

# Navigate to infrastructure directory
cd "$(dirname "$0")/../infrastructure" || exit 1

# Build and start services
echo "Building and starting services with Docker Compose..."
if command -v docker-compose &> /dev/null; then
    docker-compose up --build -d
else
    docker compose up --build -d
fi

echo ""
echo "Services started successfully!"
echo ""
echo "Service Endpoints:"
echo "  User Service:          http://localhost:8081/api/users"
echo "  Verification Service:  http://localhost:8082/api/verification"
echo ""
echo "Database Ports:"
echo "  User DB:               localhost:5432"
echo "  Verification DB:       localhost:5433"
echo ""
echo "To view logs:"
echo "  docker-compose logs -f"
echo ""
echo "To stop services:"
echo "  docker-compose down"