# Production Multi-Stage Containerization

This document details QuickTix's Docker architecture, multi-stage build optimization, and container orchestration strategy.

---

## Multi-Stage Dockerfile Architecture

The backend uses a multi-stage [`Dockerfile`](../server/Dockerfile) to isolate the Maven compilation environment from the production runtime image:

```dockerfile
# Stage 1: Build stage (Maven + JDK 25 on Alpine)
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline  # Dependency layer caching
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal production runtime (Alpine JRE 25 only)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Measured Image Size Optimization

By excluding Maven build tools, compilers, source code files, and package caches from the final image, the container size was reduced by more than half:

```text
IMAGE             TAG      DISK USAGE   CONTENT SIZE   REDUCTION
quicktix          single   870 MB       310 MB         Baseline
quicktix          multi    434 MB       137 MB         50.11% Smaller
```

* **Disk Usage**: Reduced from 870 MB down to **434 MB** (50.11% reduction).
* **Content Size**: Compressed download footprint reduced from 310 MB down to **137 MB** (55.81% reduction).
* **Attack Surface Reduction**: The runtime container contains zero build tools (`javac`, Maven), zero source code, and minimal OS utilities.

---

## Docker Compose Orchestration

The full stack is orchestrated via [`docker-compose.yml`](../docker-compose.yml):

```yaml
services:
  postgres:
    image: postgres:16
    container_name: quicktix-postgres
    ports: ["5432:5432"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d quicktix_db"]
      interval: 5s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7
    container_name: quicktix-redis
    ports: ["6379:6379"]
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5

  backend:
    build: ./server
    image: quicktix:multi
    container_name: quicktix-backend
    ports: ["8080:8080"]
    depends_on:
      postgres: { condition: service_healthy }
      redis:    { condition: service_healthy }
```

* **Healthcheck Dependencies**: The backend service only boots once PostgreSQL and Redis pass their readiness health checks, preventing connection failure startup crashes.
* **Persistent Volumes**: Database state (`postgres_data`) and Redis snapshots (`redis_data`) persist independently of container restarts.
