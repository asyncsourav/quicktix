# QuickTix Server - Spring Boot REST API

The core backend service powering QuickTix. Built with Java 25 and Spring Boot 4, it provides concurrent seat booking, distributed rate limiting, real-time WebSocket state propagation, and JWT authentication.

- **Live Backend API:** https://quicktix-api.onrender.com
- **Live Health Check:** https://quicktix-api.onrender.com/api/ping
- **Live Swagger UI:** https://quicktix-api.onrender.com/swagger-ui.html
- **Live OpenAPI Schema:** https://quicktix-api.onrender.com/v3/api-docs
- **Local Swagger UI:** http://localhost:8080/swagger-ui.html (when running locally)
- **Local OpenAPI Schema:** http://localhost:8080/v3/api-docs
- **Parent Repository:** [../README.md](../README.md)
- **Architecture Overview:** [../ARCHITECTURE.md](../ARCHITECTURE.md)

---

## Technical Architecture

- **Stateless Authentication:** Issues 15-minute access tokens in memory and 7-day refresh tokens via secure, HttpOnly cookies. Stores only SHA-256 hashes of refresh tokens in the database with atomic reuse detection and session family revocation.
- **Race Condition Prevention:** Implements JPA optimistic locking (`@Version`) on `Seat` records. When multiple concurrent requests attempt to reserve the same seat, only the first transaction commits; subsequent transactions fail version validation, trigger a rollback, and return HTTP 409 Conflict.
- **Distributed Throttling:** Utilizes Bucket4j and Lettuce over Redis. Implements a dual-bucket policy (10-burst per IP, 5-burst per user) with Resilience4j circuit breaking to fail-open if Redis encounters connectivity issues.
- **Cache-Aside Layer:** Employs Redis caching for event catalog queries (`@Cacheable`) with 5-minute TTL. Automatically invalidates caches (`@CacheEvict`) upon event creation, modification, or cancellation. Falls back gracefully to PostgreSQL if Redis is offline.
- **Real-Time Push:** Employs a Spring WebSocket STOMP broker. Seat mutations broadcast to `/topic/event.{id}`, delivering updates to connected clients in ~25 ms.
- **Non-Blocking Email:** Employs an asynchronous thread pool (`@Async`) for booking confirmation emails, isolating third-party SMTP response latency from HTTP request threads.

---

## API Summary (24 Endpoints)

| Domain | Route Patterns | Security |
|---|---|---|
| **Authentication** | `POST /api/auth/register`, `/login`, `/refresh`, `/logout` | Public (Register, Login, Refresh) |
| **Events** | `GET /api/events`, `GET /api/events/{id}`, `GET /api/events/{id}/seats` | Public reads |
| | `POST /api/events`, `PUT /api/events/{id}`, `DELETE /api/events/{id}` | Role: ORGANIZER, ADMIN |
| **Venues** | `GET /api/venues`, `GET /api/venues/{id}`, `GET /api/venues/organizer/{id}` | Public reads |
| | `POST /api/venues`, `PUT /api/venues/{id}`, `DELETE /api/venues/{id}` | Role: ORGANIZER, ADMIN |
| **Bookings** | `POST /api/bookings`, `GET /api/bookings/me`, `GET /api/bookings/{id}`, `DELETE /api/bookings/{id}` | Authenticated Users |
| **Admin** | `GET /api/admin/users`, `GET /api/admin/stats`, `GET /api/admin/bookings` | Role: ADMIN |
| **Health** | `GET /api/ping` | Public |

---

## Configuration Variables

Environment configurations are separated into `application-dev.yml` and `application-prod.yml`:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: PostgreSQL connection coordinates.
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`: Redis connection for caching and Bucket4j rate limiting.
- `JWT_SECRET`: 256-bit secret key used to sign and verify HMAC-SHA256 access tokens.
- `JWT_COOKIE_SECURE`: Controls whether the refresh cookie requires HTTPS (`true` for production, `false` for local development).
- `CORS_ALLOWED_ORIGINS`: Comma-delimited list of permitted client origins (e.g. `http://localhost:5173,https://quicktix-weld.vercel.app`).
- `SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`: SMTP server credentials for async notifications.

---

## Running Locally

### Option 1: Run with Docker Compose (Recommended)
From the repository root:
```bash
docker compose up -d --build
```

### Option 2: Run via Maven Wrapper
Ensure local PostgreSQL and Redis instances are running:
```bash
./mvnw spring-boot:run
```

---

## Automated Testing

The suite uses JUnit 5, Mockito, and Testcontainers to execute real database and cache assertions in ephemeral Docker containers:

```bash
# Run all unit and integration tests:
./mvnw test

# Generate JaCoCo code coverage report:
./mvnw test jacoco:report
```
JaCoCo HTML reports are generated at `target/site/jacoco/index.html`.
