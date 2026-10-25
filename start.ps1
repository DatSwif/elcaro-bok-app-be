$ErrorActionPreference = "Stop"

Write-Host "Building application..."
mvn clean package -DskipTests

Write-Host "Starting Docker containers..."
docker compose up --build
