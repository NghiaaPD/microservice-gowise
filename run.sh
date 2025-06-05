#!/bin/bash

set -e

# Build all Maven projects
for service in discovery-server admin-service api-gateway demo; do
  echo "Building Maven project for $service..."
  (cd $service && ./mvnw clean package -DskipTests)
done

# Build and run all services with Docker Compose
echo "Building and starting Docker containers..."
docker-compose up --build