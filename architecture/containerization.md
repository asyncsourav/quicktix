# Production Multi-Stage Containerization

**Objective:** Production-grade containerization with 50.1% image footprint reduction and healthcheck orchestration.

---

## Multi-Stage Dockerfile ([`server/Dockerfile`](../server/Dockerfile))

Separates the build environment from the minimal runtime container:

```dockerfile
# Stage 1: Build (Maven 3.9 + JDK 25 Alpine)
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime (Temurin JRE 25 Alpine)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Footprint Optimization Results

| Image Variation | Uncompressed Disk Size | Compressed Size | Reduction | Attack Surface |
| :--- | :---: | :---: | :---: | :--- |
| **Single-Stage Baseline** | 870 MB | 310 MB | Baseline | Contains JDK compiler, Maven, and build tools |
| **Multi-Stage Production** | **434 MB** | **137 MB** | **50.11% smaller** | Minimal Alpine JRE + `app.jar` only |

---

## Orchestration Architecture ([`docker-compose.yml`](../docker-compose.yml))

Healthcheck-driven startup prevents application container crashes during cold starts:

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

* **Healthchecks:** `pg_isready` and `redis-cli ping` guarantee the backend only boots when persistence sockets are ready.
* **Named Volumes:** `postgres_data` and `redis_data` ensure persistence across container rebuilds.
