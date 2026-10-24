# QuickTix Server

I built the backend REST API service for [QuickTix](../README.md) using [Java 25](https://openjdk.org/projects/jdk/25/) and [Spring Boot 4.0](https://spring.io/projects/spring-boot).

I designed the platform to handle authentication, venue administration, event publishing, automatic seat inventory generation, and transactional seat bookings with optimistic locking concurrency controls to guarantee that seats can never be double-booked.

**Interactive API Documentation (Swagger UI):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
**OpenAPI Specification:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)  
**Architecture Deep Dives:** [../architecture/](../architecture/README.md)  
**Verification Scripts:** [../scripts/](../scripts/README.md)

---

## Key Architectural Systems I Built

* **Optimistic Concurrency Control**: I implemented Hibernate `@Version` collision handling on each seat entity to guarantee zero double-bookings under concurrent ticket spikes. (See [Concurrency Control Deep Dive](../architecture/concurrency-control.md))
* **Distributed In-Memory Caching**: I cached hot read paths (`GET /api/events`) in Redis 7 with a 5-minute TTL and automated `@CacheEvict` invalidation. I built fail-open error handling via [`CacheErrorConfig.java`](src/main/java/com/asyncsourav/quicktix/config/CacheErrorConfig.java) to ensure catalog reads fall back to PostgreSQL if Redis times out. (See [Distributed Caching Deep Dive](../architecture/distributed-caching.md))
* **Anti-Scalping Distributed Rate Limiting**: I built Bucket4j + Lettuce token bucket rate limiting on `POST /api/bookings` enforcing per-user (burst 5) and per-IP (burst 10) limits with short 200ms timeouts and fail-open resilience. (See [Rate Limiting Deep Dive](../architecture/distributed-rate-limiting.md))
* **Dual-Token Authentication**: I designed stateless 15-minute access tokens in memory paired with 7-day rotating refresh tokens in Secure, HttpOnly cookies. Full database tracking with SHA-256 token hashes detects replay attacks and revokes compromised token families. (See [Security Architecture Deep Dive](../architecture/security-and-auth.md))
* **Multi-Stage Containerization**: I cut the production container size by 50.1% (from 870 MB down to 434 MB) using a multi-stage Docker build with Eclipse Temurin JRE 25 on Alpine Linux. (See [Containerization Deep Dive](../architecture/containerization.md))

---

## Package Structure

```
com.asyncsourav.quicktix/
├── config/           # SecurityConfig, CorsProperties, RedisConfig, RateLimiterConfig, CacheErrorConfig
├── controller/       # Auth, Event, Venue, Booking, Admin, Ping controllers
├── dto/
│   ├── common/       # ApiResponse<T>, ErrorResponse
│   ├── request/      # RegisterRequest, LoginRequest, RefreshTokenRequest, EventRequest, BookingRequest
│   └── response/     # AuthResponse, UserResponse, EventResponse, BookingResponse, StatsResponse
├── entity/           # User, Venue, Event, Seat (@Version), Booking, RefreshToken
├── exception/        # Custom domain exceptions & GlobalExceptionHandler (@RestControllerAdvice)
├── repository/       # Spring Data JPA repositories with atomic native queries
├── security/         # JwtUtil, JwtAuthFilter, CustomUserDetailsService
└── service/          # AuthService, EventService, VenueService, BookingService, RateLimiterService
```

---

## Local Development Setup

### Prerequisites
* [Java 25 JDK](https://openjdk.org/projects/jdk/25/)
* [PostgreSQL 16](https://www.postgresql.org/) (running on port `5432`)
* [Redis 7](https://redis.io/) (running on port `6379`)

### 1. Database & Cache Initialization
If running dependencies via Docker:
```bash
docker compose up -d postgres redis
```

### 2. Environment Configuration
Copy `.env.example` to `.env` in the repository root:
```properties
PORT=8080
DB_URL=jdbc:postgresql://localhost:5432/quicktix_db?options=-c%20timezone%3DUTC
DB_USERNAME=postgres
DB_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000,http://localhost:8080
```

### 3. Build & Run Application
Using the bundled Maven wrapper:

* **Compile:**
  ```bash
  ./mvnw compile
  ```
* **Run Unit Tests:**
  ```bash
  ./mvnw test
  ```
* **Package Executable JAR:**
  ```bash
  ./mvnw clean package -DskipTests
  ```
* **Launch Server:**
  ```bash
  ./mvnw spring-boot:run
  ```

---

## Running with Docker Compose

To run the complete production stack (PostgreSQL, Redis, and Backend):
```bash
docker compose up -d --build
```
Inspect logs:
```bash
docker compose logs -f backend
```

---

## API Endpoints Overview

All 24 REST API endpoints are documented with interactive schemas, live execution, and sample payloads in Swagger UI at:
**[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

Primary endpoint groups:
* `POST /api/auth/*` - Register, login, silent refresh, and family logout.
* `GET, POST, PUT, DELETE /api/events/*` - Event catalog, seat availability queries, event publishing.
* `GET, POST /api/venues/*` - Venue capacity management and layout configuration.
* `POST, GET, DELETE /api/bookings/*` - Concurrency-safe seat reservations, user booking history, cancellations.
* `GET /api/admin/*` - Platform metrics, user directories, and system ledgers.
* `GET /api/ping` - Service liveness probe.

---

## Author

**Sourav Kumar**
* **GitHub:** [https://github.com/asyncsourav](https://github.com/asyncsourav)
* **LinkedIn:** [https://www.linkedin.com/in/asyncsourav](https://www.linkedin.com/in/asyncsourav)
* **LeetCode:** [https://leetcode.com/u/asyncsourav](https://leetcode.com/u/asyncsourav)
* **Email:** [xsouravkumar357@gmail.com](mailto:xsouravkumar357@gmail.com)
