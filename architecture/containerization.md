# Production Multi-Stage Containerization

This document details how I containerized QuickTix, optimized the production Docker image by 50.1%, and orchestrated the backend, database, and cache services using Docker Compose.

---

## Why I Built a Multi-Stage Dockerfile

When I first containerized the Spring Boot backend, I created a standard single-stage image. However, inspecting the output with `docker images` revealed a major efficiency problem: the image weighed in at **870 MB**. 

Packing the full Maven toolchain, compiler utilities, intermediate build caches, and source files into production:
1. Drastically slowed down image pull and deployment times.
2. Unnecessarily increased disk consumption.
3. Expanded the security attack surface by leaving build tools and compilers inside the production container.

To solve this, I designed a multi-stage [`Dockerfile`](../server/Dockerfile) that cleanly separates the compilation environment from the runtime container:

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

## The Optimization Results I Measured

After building both variations, I compared their disk footprints:

```text
IMAGE             TAG      DISK USAGE   CONTENT SIZE   REDUCTION
quicktix          single   870 MB       310 MB         Baseline
quicktix          multi    434 MB       137 MB         50.11% Smaller
```

* **Local Disk Footprint:** Cut from 870 MB down to **434 MB** (a **50.11% reduction**).
* **Compressed Download Footprint:** Cut from 310 MB down to **137 MB** (a **55.81% reduction**).
* **Attack Surface Elimination:** The final image contains zero build tools (`javac`, Maven), zero raw Java source files, and only the minimal Alpine JRE runtime required to execute `app.jar`.

---

## How I Orchestrated the Stack with Docker Compose

To run the entire platform with one command, I wrote [`docker-compose.yml`](../docker-compose.yml):

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

* **Startup Healthchecks:** I added explicit container healthchecks for PostgreSQL (`pg_isready`) and Redis (`redis-cli ping`). By binding the backend's `depends_on` to `service_healthy`, I prevented startup crashes caused by Spring Boot attempting to connect before the database sockets were listening.
* **Persistent Named Volumes:** I configured persistent volumes (`postgres_data`, `redis_data`) to ensure event catalogs, user registrations, and seat reservations persist across container rebuilds.
