#!/bin/bash
set -e

echo "Building application..."
mvn clean package -DskipTests

echo "Starting Docker containers..."
docker compose up --build
