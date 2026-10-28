# QuickTix Server

The Spring Boot REST API behind [QuickTix](../README.md). It handles authentication, venues, events, automatic seat generation, bookings and the real-time seat updates.

**Swagger UI:** http://localhost:8080/swagger-ui.html · **OpenAPI JSON:** http://localhost:8080/v3/api-docs

## Stack

Java 25, Spring Boot 4, Spring Security, Spring Data JPA (Hibernate 7), PostgreSQL 16, Redis 7 (Spring Cache and Bucket4j with Lettuce), Spring WebSocket (STOMP), Spring Mail, springdoc-openapi, Maven.

## Run it

**With Docker (backend, PostgreSQL and Redis together)**, from the repository root:

```bash
cp .env.example .env     # then set JWT_SECRET
docker compose up -d --build
docker compose logs -f backend
```

**From the IDE or Maven**, with only the dependencies in Docker:

```bash
docker compose up -d postgres redis
cd server
./mvnw spring-boot:run
```

Run the tests with `./mvnw test`.

## Configuration

Settings are read from environment variables, with a `dev` and a `prod` profile (`application-dev.yml`, `application-prod.yml`).

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `REDIS_HOST`, `REDIS_PORT` | Redis connection (used for cache and rate limiting) |
| `JWT_SECRET` | Signing key for access tokens. Required, use a long random value |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins, also applied to the WebSocket endpoint |
| `JWT_COOKIE_SECURE` | `false` for local HTTP, `true` when served over HTTPS |

Email uses Spring's standard `spring.mail.*` properties (SMTP host, username, password). Keep credentials in `.env`, never in the repository.

## API overview

24 endpoints, all documented in Swagger UI.

| Group | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `/login`, `/refresh`, `/logout` |
| Venues | `GET /api/venues`, `GET /api/venues/{id}`, `GET /api/venues/organizer/{organizerId}`, `POST /api/venues`, `PUT` and `DELETE /api/venues/{id}` |
| Events | `GET /api/events` (optional `category` and `search`), `GET /api/events/{id}`, `GET /api/events/{id}/seats`, `POST /api/events`, `PUT` and `DELETE /api/events/{id}` |
| Bookings | `POST /api/bookings`, `GET /api/bookings/me`, `GET /api/bookings/{id}`, `DELETE /api/bookings/{id}` |
| Admin | `GET /api/admin/users`, `/stats`, `/bookings` |
| Health | `GET /api/ping` |

Roles are `ADMIN`, `ORGANIZER` and `USER`. An event can only be changed or deleted by its organizer or an admin, and an organizer can only schedule events at venues they own.

**Responses.** Successful responses use an `ApiResponse<T>` envelope. Errors go through a global `@RestControllerAdvice` and return one shape:

```json
{
  "timestamp": "2026-10-24T04:50:30",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Too many booking requests. Please wait 1 second before trying again."
}
```

## How the important parts work

**Booking and concurrency.** `Seat` has a `@Version` field. A booking changes all requested seats in one `@Transactional` method. If another request got to a seat first, Hibernate throws an optimistic locking failure, the whole booking rolls back (no partial bookings) and the API returns `409`.

**Authentication.** Login returns a 15-minute access token in the response body and sets a refresh token cookie (`HttpOnly`, `Path=/api/auth`, 7 days). The database stores only the SHA-256 hash of each refresh token plus a session family ID. `POST /api/auth/refresh` claims the current token with a single atomic update (`WHERE used = false`), then issues a new pair in the same family. A token that is already used or revoked triggers reuse detection: the whole family is revoked and the cookie is cleared. `POST /api/auth/logout` revokes the family. The reuse-detection method uses `@Transactional(noRollbackFor = BadCredentialsException.class)` so the revoke is not rolled back by the exception that follows it.

**Caching.** `GET /api/events` and `GET /api/events/{id}` use `@Cacheable` (cache names `events` and `event`, 5-minute TTL). Create, update and delete evict them. `CacheErrorConfig` logs cache errors and carries on, so a Redis outage means slower reads from PostgreSQL, not failures. Seat availability is never cached.

**Rate limiting.** An interceptor on `POST /api/bookings` applies Bucket4j token buckets stored in Redis: per user (burst 5, refill 1 per second) and per IP (burst 10, refill 2 per second). Redis timeouts are short (200 ms) and any Redis failure lets the request through. Limit hits throw `RateLimitExceededException`, handled with `Retry-After`.

**Real-time updates.** `WebSocketConfig` exposes `/ws` (SockJS and native WebSocket) with a simple in-memory broker on `/topic`. After a booking or cancellation, `SeatBroadcastService` publishes the seat change to `/topic/event.{eventId}`. The in-memory broker only reaches clients connected to the same instance, so running several instances would need a shared broker such as Redis pub/sub.

**Email.** `EmailService` sends booking and cancellation emails with `@Async` on a dedicated executor, using small detached DTOs so no entity or database session is touched off the request thread.

## Package structure

```
com.asyncsourav.quicktix/
├── config/       Security, CORS, Redis, rate limiter, WebSocket, OpenAPI, async executor, data seeding
├── controller/   Auth, Event, Venue, Booking, Admin, Ping
├── dto/          common (ApiResponse, ErrorResponse), request, response, email
├── entity/       User, Venue, Event, Seat, Booking, RefreshToken and their enums
├── exception/    Domain exceptions and GlobalExceptionHandler
├── repository/   Spring Data JPA repositories
├── security/     JwtUtil, JwtAuthFilter, CustomUserDetailsService
└── service/      Auth, Event, Venue, Booking, Admin, RateLimiter, SeatBroadcast, Email
```

## Docker image

The `Dockerfile` has two stages: Maven with a full JDK builds the jar, and a JRE-only Alpine image runs it. The final image is about 434 MB compared with 870 MB for a single-stage build of the same app. A `.dockerignore` keeps `target/`, `.git` and local files out of the build.

## Tests

`./mvnw test` runs the 26-test suite (JUnit 5, Mockito, and Testcontainers against real PostgreSQL and Redis containers).

- **Testcontainers Integration Tests (`BookingIntegrationTest`):**
  - Full end-to-end booking lifecycle and cancellation workflows with seat state and transaction rollback verification.
- **Service Layer Unit Tests:** High-risk business logic across `BookingService` (81.6% line coverage), `AuthService` (91.7% line coverage), and `EmailService` (58.8% line coverage).
- **Coverage (JaCoCo):**
  - **43.2% line coverage (41.6% instruction coverage)** on service-layer logic.
  - **39.8% line coverage** across the overall application bundle.
  - Excludes boilerplate by design (DTOs, JPA entities, configuration classes, and application bootstrap). Run `./mvnw verify` or `./mvnw test` to generate the interactive report at `target/site/jacoco/index.html`.