# QuickTix

A production-grade, distributed event ticketing and seat reservation platform built with [Java 25](https://openjdk.org/projects/jdk/25/), [Spring Boot 4](https://spring.io/projects/spring-boot), [PostgreSQL 16](https://www.postgresql.org/), [Docker](https://www.docker.com/), and [React](https://react.dev/).

QuickTix addresses high-concurrency seat booking scenarios where multiple users attempt to reserve the same seat simultaneously. The platform guarantees strict consistency and zero double-bookings through database-level transactions and optimistic locking.

**Live Deployment:** [https://quicktix-weld.vercel.app/](https://quicktix-weld.vercel.app/)

<!-- SCREENSHOT: QuickTix Web Application Homepage and Event Catalog Banner -->

---

## Architecture Overview

QuickTix operates as a decoupled client-server architecture:

```
[ Client: React + Vite ] 
       | (HTTPS / REST)
       v
[ Reverse Proxy / Cloud Gateway ]
       | (Port 8080)
       v
[ Spring Boot 4 Backend Application ]
   ├── Spring Security (Stateless JWT Filter)
   ├── Controller Layer (22 REST Endpoints, OpenAPI 3.0)
   ├── Service Layer (Business Rules & Ownership Enforcement)
   └── Persistence Layer (Spring Data JPA / Hibernate ORM)
       └── Database Concurrency (@Version Optimistic Locking)
               |
               v (JDBC Pool - HikariCP)
[ PostgreSQL 16 Database ] (Containerized / Persistent Volume)
```

---

## Core Engineering Highlight: Concurrency Control

Ticket booking platforms encounter severe concurrency bottlenecks during high-demand on-sale windows. Without robust concurrency controls, simultaneous requests result in race conditions, phantom reads, and double-sold seats.

### Concurrency Mechanism
QuickTix implements **Optimistic Concurrency Control (OCC)** combined with declarative database transactions:

1. **Entity Versioning**: The `Seat` entity incorporates a `@Version private Long version;` field managed by [Hibernate](https://hibernate.org/orm/).
2. **Atomic Batch Processing**: Seat reservations execute within a `@Transactional` boundary across a collection of seat IDs (`List<Long> seatIds`).
3. **Collision Detection**: When concurrent requests attempt to reserve the same seat, the first committing transaction succeeds and increments the version column (`UPDATE seats SET status = 'BOOKED', version = version + 1 WHERE id = ? AND version = ?`).
4. **Automated Rollback**: Conflicting concurrent transactions fail the version predicate, triggering an `ObjectOptimisticLockingFailureException`. The Spring transaction manager immediately rolls back all seat modifications in the batch.
5. **Deterministic Error Handling**: A centralized `GlobalExceptionHandler` intercepts the collision and produces a standardized `409 Conflict` response with human-readable diagnostic details.

<!-- SCREENSHOT: Concurrency Benchmark Results showing 10 simultaneous requests with 1 winner (HTTP 201) and 9 conflict rejections (HTTP 409) -->

### Empirical Verification (Race Condition Test)
A high-concurrency simulation dispatching 10 parallel requests (`Promise.all`) for the exact same seat within 1 millisecond yields deterministic isolation:

| Request Index | HTTP Status | Response Payload | Result |
| :---: | :---: | :--- | :--- |
| Request 1 | **201 Created** | `{"success": true, "message": "Booking confirmed successfully"}` | **Seat Reserved** |
| Request 2 | **409 Conflict** | `{"status": 409, "message": "This seat was just selected or booked..."}` | Rejected |
| Request 3 | **409 Conflict** | `{"status": 409, "message": "This seat was just selected or booked..."}` | Rejected |
| Request 4 | **409 Conflict** | `{"status": 409, "message": "This seat was just selected or booked..."}` | Rejected |
| Request 5 | **409 Conflict** | `{"status": 409, "message": "This seat was just selected or booked..."}` | Rejected |
| Request 6 | **409 Conflict** | `{"status": 409, "message": "This seat was just selected or booked..."}` | Rejected |
| Request 7 | **409 Conflict** | `{"status": 409, "message": "This seat was just selected or booked..."}` | Rejected |
| Request 8 | **409 Conflict** | `{"status": 409, "message": "This seat was just selected or booked..."}` | Rejected |
| Request 9 | **409 Conflict** | `{"status": 409, "message": "This seat was just selected or booked..."}` | Rejected |
| Request 10 | **409 Conflict** | `{"status": 409, "message": "This seat was just selected or booked..."}` | Rejected |

---

## Technology Stack

### Backend Infrastructure
* **Language & Runtime:** [Java 25](https://openjdk.org/projects/jdk/25/) (Temurin OpenJDK)
* **Framework:** [Spring Boot 4.0.x](https://spring.io/projects/spring-boot)
  * Spring Web (RESTful API architecture)
  * Spring Data JPA / [Hibernate 7.2](https://hibernate.org/orm/) (ORM and persistence)
  * [Spring Security 7.0](https://spring.io/projects/spring-security) (Stateless authentication filter chain)
  * Jakarta Bean Validation (Declarative input validation)
* **Database:** [PostgreSQL 16](https://www.postgresql.org/) (ACID-compliant relational engine)
* **Connection Pool:** [HikariCP](https://github.com/brettwooldridge/HikariCP)
* **Security & Tokens:** [JJWT (Java JSON Web Token) 0.12.6](https://github.com/jwtk/jjwt) (HS256 signature scheme) + BCrypt password hashing
* **API Documentation:** [springdoc-openapi 2.8.5](https://springdoc.org/) (Swagger UI 3.0 specification)
* **Containerization:** [Docker](https://www.docker.com/) & [Docker Compose v2](https://docs.docker.com/compose/)

### Frontend Client
* **Framework & Tooling:** [React 19](https://react.dev/), [Vite 6](https://vitejs.dev/)
* **Routing:** [React Router 6](https://reactrouter.com/) (Role-protected route guards)
* **HTTP Client:** [Axios](https://axios-http.com/) (Configured request/response interceptors for Bearer auth)
* **Styling & Design System:** Custom Vanilla CSS Design System with theme tokens (Light / Dark mode toggle)
* **Hosting:** [Vercel](https://vercel.com/) (Edge CDN deployment with SPA client rewrite rules)

---

## Repository Architecture

```
quicktix/
├── client/                     # Frontend Single Page Application (React + Vite)
│   ├── src/                    # Components, pages, context providers, API client
│   ├── vercel.json             # Vercel SPA routing rewrites
│   └── package.json            # Node.js dependencies
├── server/                     # Backend REST API Service (Spring Boot 4)
│   ├── src/main/java/          # Controllers, services, JPA repositories, entities, security
│   ├── src/main/resources/     # Profiles (application.yml, application-dev, application-prod)
│   ├── Dockerfile              # Multi-stage optimized production build
│   └── pom.xml                 # Maven build configuration (Java 25)
├── docker-compose.yml          # Full-stack container orchestration
├── .env.example                # Environment variable specification template
└── README.md                   # Project documentation
```

---

## Quick Start (Containerized Stack)

The entire backend and database stack can be spun up with a single command using Docker Compose.

### Prerequisites
* [Docker Desktop](https://www.docker.com/products/docker-desktop/) (v24.0 or newer)
* [Node.js](https://nodejs.org/) (v18.0 or newer, for running client locally)

### 1. Clone Repository & Setup Environment
```bash
git clone https://github.com/asyncsourav/quicktix.git
cd quicktix

# Copy environment variables
cp .env.example .env
```

### 2. Start Full Backend Stack (PostgreSQL + Spring Boot)
```bash
docker compose up -d --build
```

Docker Compose will:
1. Boot PostgreSQL 16 with a persistent named volume (`postgres_data`).
2. Run healthchecks via `pg_isready` until the database is ready to accept connections.
3. Build the backend using a multi-stage Docker build with Java 25.
4. Launch the Spring Boot application on port `8080`.
5. Seed demo accounts, venues, events, and seat maps automatically on first run.

Verify that both containers are running and healthy:
```bash
docker compose ps
```

### 3. Launch Frontend Client
```bash
cd client
npm install
npm run dev
```
The client application will start at `http://localhost:5173` (or `http://localhost:3000`).

---

## Interactive API Documentation

QuickTix features full interactive OpenAPI 3.0 documentation via Swagger UI.

* **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI Specification JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

<!-- SCREENSHOT: Swagger UI documentation showing 24 endpoints organized across Auth, Events, Venues, Bookings, Admin, and Health -->

### API Endpoint Registry (24 Endpoints)

| Domain | Method | Endpoint | Authorization | Description |
| :--- | :---: | :--- | :---: | :--- |
| **Auth** | `POST` | `/api/auth/register` | Public | Register new account with BCrypt password hashing |
| | `POST` | `/api/auth/login` | Public | Authenticate credentials and receive dual token pair |
| | `POST` | `/api/auth/refresh` | Public | Rotate refresh token with token-family reuse detection |
| | `POST` | `/api/auth/logout` | Public | Revoke active refresh token session family |
| **Events** | `GET` | `/api/events` | Public | Query events with category and keyword search filters |
| | `GET` | `/api/events/{id}` | Public | Retrieve detailed event metadata |
| | `GET` | `/api/events/{id}/seats` | Public | Fetch current seat layout and availability status |
| | `POST` | `/api/events` | Organizer / Admin | Publish event and auto-generate seat layout |
| | `PUT` | `/api/events/{id}` | Organizer / Admin | Update event details (organizer ownership enforced) |
| | `DELETE` | `/api/events/{id}` | Organizer / Admin | Delete event (organizer ownership enforced) |
| **Venues** | `GET` | `/api/venues` | Public | List all registered venues |
| | `GET` | `/api/venues/{id}` | Public | Retrieve venue specifications and capacity |
| | `GET` | `/api/venues/organizer/{id}` | Public | List venues managed by an organizer |
| | `POST` | `/api/venues` | Organizer / Admin | Register new venue |
| | `PUT` | `/api/venues/{id}` | Organizer / Admin | Update venue details (ownership enforced) |
| | `DELETE` | `/api/venues/{id}` | Organizer / Admin | Remove venue (ownership enforced) |
| **Bookings**| `POST` | `/api/bookings` | Authenticated | Atomic multi-seat reservation with optimistic locking |
| | `GET` | `/api/bookings/me` | Authenticated | Retrieve authenticated user booking history |
| | `GET` | `/api/bookings/{id}` | Authenticated | Retrieve booking receipt (ownership verified) |
| | `DELETE` | `/api/bookings/{id}` | Authenticated | Cancel booking and release seats back to inventory |
| **Admin** | `GET` | `/api/admin/users` | Admin | Administrative user directory audit |
| | `GET` | `/api/admin/stats` | Admin | Real-time aggregate revenue and utilization metrics |
| | `GET` | `/api/admin/bookings` | Admin | Platform-wide transaction ledger audit |
| **Health** | `GET` | `/api/ping` | Public | Service liveness probe |

---

## Security Architecture: Dual Token Rotation & Reuse Detection

QuickTix implements **Rotating Refresh Tokens with Session Family Tracking and Automated Reuse Detection**:

1. **Short-Lived Access Tokens (15 Minutes)**: Stateless JWTs carry user identity and permissions to backend API requests.
2. **Opaque Long-Lived Refresh Tokens (7 Days)**: Stored in the database as **SHA-256 hashes** (`refresh_tokens` table) to prevent token compromise in the event of a database dump.
3. **Token Rotation**: Every call to `POST /api/auth/refresh` invalidates the submitted refresh token (`used = true`) and issues a brand-new access token and refresh token belonging to the same `family_id`.
4. **Token Family Reuse Detection**: If an already-used or revoked refresh token is presented again (indicating token interception or replay attacks), the server immediately revokes **all tokens in that family** (`revoked = true`). Both the attacker and the victim are logged out, preventing unauthorized access.
5. **Silent Client Refresh**: The React client uses an Axios response interceptor that transparently queues outgoing requests on HTTP 401, calls `/api/auth/refresh`, and replays requests without disrupting the user experience.

---

## Production Docker Optimization

The backend employs an optimized multi-stage `Dockerfile`:

```dockerfile
# Stage 1: Build stage with compiler and dependencies
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage with minimal Alpine JRE
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

<!-- SCREENSHOT: Terminal output of 'docker images quicktix' comparing quicktix:single vs quicktix:multi -->

### Container Metric Comparison
* **Single-Stage Build (Legacy baseline):** 870 MB
* **Multi-Stage Build (Production):** 434 MB
* **Image Size Reduction:** **50.11%** (436 MB eliminated from production container)
* **Content Layer Size Reduction:** **55.81%** (from 310 MB to 137 MB)

---

## Seed Data & Demo Credentials

When launched against an uninitialized database, `DataInitializer` seeds test accounts with password `password123`:

| Role | Email | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@quicktix.com` | `password123` | System oversight, aggregate metrics, platform audit |
| **Organizer** | `organizer@quicktix.com` | `password123` | Venue management, event creation, seat generation |
| **Attendee (User)** | `user@quicktix.com` | `password123` | Event browsing, seat reservation, booking management |

---

## Engineering Roadmap

- [x] Phase 1: Core Domain Model, JPA Repositories, REST API Endpoints
- [x] Phase 2: Role-Based Access Control (RBAC), Stateless JWT Security, Password Hashing
- [x] Phase 3: Optimistic Locking Concurrency Control (`@Version`), Race Condition Verification
- [x] Phase 4: OpenAPI 3.0 / Swagger UI Interactive Documentation (`springdoc-openapi`)
- [x] Phase 5: Multi-Stage Docker Containerization, Full-Stack Docker Compose Orchestration
- [x] Phase 6: Dual rotating JWT refresh tokens with token family reuse detection
- [ ] Phase 7: Distributed caching with Redis for hot read paths (event listings, seat maps)
- [ ] Phase 8: Distributed API rate limiting via Bucket4j and token bucket algorithms
- [ ] Phase 9: Real-time seat reservation broadcasts over WebSocket / STOMP protocol
- [ ] Phase 10: Automated test coverage (JUnit 5, Mockito, Testcontainers for PostgreSQL)
- [ ] Phase 11: Continuous Integration pipeline via GitHub Actions
- [ ] Phase 12: High-load performance testing via k6

---

## Author

**Sourav Kumar**
* **GitHub:** [https://github.com/asyncsourav](https://github.com/asyncsourav)
* **LinkedIn:** [https://www.linkedin.com/in/asyncsourav](https://www.linkedin.com/in/asyncsourav)
* **LeetCode:** [https://leetcode.com/u/asyncsourav](https://leetcode.com/u/asyncsourav)
* **Email:** [xsouravkumar357@gmail.com](mailto:xsouravkumar357@gmail.com)
