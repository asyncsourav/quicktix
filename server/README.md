# QuickTix Server

The Spring Boot REST API behind [QuickTix](../README.md), an event ticket booking platform. It handles authentication, venues, events, seat maps and bookings, and guarantees that a seat can never be double-booked.

## Tech stack
- **Java 25**, **Maven**
- **Spring Boot 4.0.x**: Spring Web, Spring Data JPA (Hibernate), Validation, Spring Security
- **PostgreSQL 16** (via Docker Compose)
- **JWT (HS256)** authentication with BCrypt password hashing
- **springdoc-openapi (2.8.5)** for Swagger UI interactive docs
- **Lombok** and **Spring Boot DevTools**

## Domain model
| Entity | Key fields |
|---|---|
| `User` | `id`, `name`, `email` (unique), `passwordHash`, `role` (`ADMIN`, `ORGANIZER`, `USER`) |
| `Venue` | `id`, `name`, `address`, `totalCapacity`, `organizer` (User FK) |
| `Event` | `id`, `title`, `description`, `venue` (Venue FK), `startTime`, `category`, `basePrice`, `organizer` (User FK) |
| `Seat` | `id`, `event` (Event FK), `seatLabel`, `price`, `status` (`AVAILABLE`, `HELD`, `BOOKED`), `@Version` |
| `Booking` | `id`, `user` (User FK), `event` (Event FK), `seats`, `totalAmount`, `status`, `createdAt` |

Entities are never exposed directly: every endpoint uses strictly-typed request and response DTOs with Jakarta Bean Validation.

## API overview
22 endpoints grouped into 6 tagged sections in Swagger UI:

| Group | Endpoints | Access |
|---|---|---|
| **Auth** | `POST /api/auth/register`, `POST /api/auth/login` | **Public** |
| **Events** | `GET /api/events` (filters: `category`, `search`), `GET /api/events/{id}`, `GET /api/events/{id}/seats` | **Public** |
| | `POST /api/events`, `PUT /api/events/{id}`, `DELETE /api/events/{id}` | **ORGANIZER** (own events), **ADMIN** |
| **Venues** | `GET /api/venues`, `GET /api/venues/{id}`, `GET /api/venues/organizer/{organizerId}` | **Public** |
| | `POST /api/venues`, `PUT /api/venues/{id}`, `DELETE /api/venues/{id}` | **ORGANIZER**, **ADMIN** |
| **Bookings** | `POST /api/bookings`, `GET /api/bookings/me`, `GET /api/bookings/{id}`, `DELETE /api/bookings/{id}` | **Authenticated** (own bookings) |
| **Admin** | `GET /api/admin/users`, `GET /api/admin/stats`, `GET /api/admin/bookings` | **ADMIN** |
| **Health** | `GET /api/ping` | **Public** |

### Standard Response Envelopes

All success responses are wrapped in a generic `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "message": "Booking confirmed successfully",
  "data": {
    "id": 1,
    "userId": 3,
    "userName": "Alex Morgan",
    "eventId": 1,
    "eventTitle": "Coldplay Music of the Spheres Tour",
    "seats": [
      {
        "id": 10,
        "eventId": 1,
        "seatLabel": "A1",
        "price": 89.99,
        "status": "BOOKED"
      }
    ],
    "totalAmount": 89.99,
    "status": "CONFIRMED",
    "createdAt": "2026-10-12T17:30:00"
  },
  "timestamp": "2026-10-12T17:30:00.124819"
}
```

Errors are handled centrally by `@RestControllerAdvice` (`GlobalExceptionHandler`), returning a consistent `ErrorResponse` structure:

```json
{
  "timestamp": "2026-10-12T17:30:00.412891",
  "status": 409,
  "error": "Conflict",
  "message": "Seat A1 is already booked or held. Please choose an available seat.",
  "path": "/api/bookings"
}
```

## How double-booking is prevented
1. `POST /api/bookings` receives an `eventId` and list of `seatIds`.
2. Inside a single `@Transactional` method, every requested seat is retrieved and verified to be `AVAILABLE`.
3. Seats are transitioned to `BOOKED` and saved. Each `Seat` entity contains a `@Version` column.
4. If another concurrent transaction modified any of those seats first, Hibernate detects the version mismatch and throws an `OptimisticLockException`.
5. `GlobalExceptionHandler` intercepts the exception and returns a clean `409 Conflict` JSON response instead of a raw 500 error or stack trace.
6. The transaction is **all-or-nothing**: if even a single seat in the batch cannot be booked, the entire booking rolls back.

## Getting started
Prerequisites: Java 25, Docker. (Maven wrapper is included).

```bash
cd server

# 1. Start PostgreSQL (postgres:16 on port 5432, persisted in named volume)
docker compose up -d

# 2. Run the Spring Boot API
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

## Configuration
Configuration is defined in `application.yml` and profile-specific files (`application-dev.yml` and `application-prod.yml`):

| Variable | Description | Local Dev Default |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/quicktix_db?options=-c%20timezone%3DUTC` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | 256-bit secret key for signing HS256 tokens | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` |
| `JWT_EXPIRATION_MS` | JWT token lifetime in milliseconds | `86400000` (24 hours) |
| `PORT` | Web server listening port | `8080` |

## API documentation (Swagger UI)
With the application running:
* **Interactive Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **Raw OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Endpoints are ordered logically (**Auth**, **Events**, **Venues**, **Bookings**, **Admin**, **Health**). To test protected endpoints, log in using `POST /api/auth/login`, copy the token, click the **Authorize** lock button in Swagger UI, and paste it in.

## Project structure
```
server/
├── src/main/java/com/asyncsourav/quicktix/
│   ├── config/       # SecurityConfig, OpenApiConfig, DataInitializer
│   ├── security/     # JwtUtil, JwtAuthFilter, CustomUserDetailsService
│   ├── controller/   # Auth, Event, Booking, Venue, Admin, Ping controllers
│   ├── service/      # Business logic, ownership validation, booking transactions
│   ├── repository/   # Spring Data JPA repositories
│   ├── entity/       # User, Venue, Event, Seat (@Version), Booking
│   ├── dto/          # Request, Response, and Common ApiResponse/ErrorResponse records
│   └── exception/    # Custom exceptions & GlobalExceptionHandler
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-prod.yml
├── Dockerfile        # Multi-stage container build
├── pom.xml
└── mvnw
```

## Roadmap
- [x] Swagger / OpenAPI documentation
- [ ] Multi-stage Dockerfile and full-stack Docker Compose
- [ ] Dual rotating JWT refresh tokens with reuse detection
- [ ] Redis caching (with measured latency improvement)
- [ ] Distributed rate limiting with Bucket4j
- [ ] WebSocket/STOMP live seat-map broadcasts
- [ ] JUnit 5, Mockito and Testcontainers tests; JaCoCo coverage report
- [ ] GitHub Actions CI
- [ ] k6 load test with recorded results
- [ ] Managed Postgres and Redis deployment; backend on Render with `/actuator/health`

## Author
**Sourav Kumar**: [GitHub](https://github.com/asyncsourav) · [LinkedIn](https://www.linkedin.com/in/asyncsourav) · [LeetCode](https://leetcode.com/u/asyncsourav) · [Email](mailto:xsouravkumar357@gmail.com)
