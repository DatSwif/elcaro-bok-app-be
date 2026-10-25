# elcaro-bok-app-be

## Prerequisites

- JDK 25
- Maven 3.x
- Docker and Docker Compose

## Quickstart

Linux:
```bash
./start.sh
```

Windows:
```powershell
./start.ps1
```

This builds the JAR and starts the app + PostgreSQL via Docker Compose.

| Service    | URL                                   |
|------------|---------------------------------------|
| API        | http://localhost:8080                 |
| Swagger UI | http://localhost:8080/swagger-ui.html |.
