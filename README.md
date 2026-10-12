# QuickTix

A full-stack event ticket booking platform. Organizers publish events at venues, users pick seats from a live seat map and book them, and the backend guarantees that two people can never book the same seat.

**Live demo:** [https://quicktix-weld.vercel.app/](https://quicktix-weld.vercel.app/)

<!-- TODO: add a Swagger UI screenshot here, e.g. ![Swagger UI](docs/swagger-ui.png) -->

## Why this project
Ticket booking is a concurrency problem in disguise. When two users click the same seat at the same moment, exactly one must win and the other must get a clean "seat just got taken" response. QuickTix handles this with `@Transactional` bookings and `@Version`-based optimistic locking, with all-or-nothing behaviour: if any seat in a request fails, the whole booking rolls back.

## Tech stack
| Layer | Technology |
|---|---|
| Frontend | React, Vite, Tailwind CSS, React Router, Axios |
| Backend | Java 25, Spring Boot 4.0.x, Spring Web, Spring Data JPA (Hibernate), Spring Security, Bean Validation, Lombok |
| Database | PostgreSQL 16 (via Docker Compose) |
| Auth | JWT (HS256), BCrypt password hashing, role-based access (ADMIN, ORGANIZER, USER) |
| API docs | springdoc-openapi (Swagger UI) |
| Hosting | Frontend on Vercel |

## Features
- Register and log in; JWT-secured API with three roles: `ADMIN`, `ORGANIZER`, `USER`
- Organizers and admins manage venues and events; organizers can only edit their own events (ownership checked in the service layer)
- Event listing with filters (category, search keyword)
- Seats auto-generated from venue capacity when an event is created, with a per-event seat map
- Atomic multi-seat booking with optimistic locking, plus listing and cancelling your own bookings
- Consistent JSON error responses via a global exception handler
- Admin endpoints for platform statistics, user listing and booking oversight
- Interactive API documentation with Swagger UI: 22 endpoints in 6 groups, with JWT login via the Authorize button

## Repository structure
```
quicktix/
├── client/        # React + Vite frontend -> see client/README.md
├── server/        # Spring Boot REST API -> see server/README.md
└── README.md      # you are here
```

## Quick start
Prerequisites: Java 25, Maven, Node.js 18+, Docker.

```bash
# 1. Clone repository
git clone https://github.com/asyncsourav/quicktix.git
cd quicktix

# 2. Start PostgreSQL
cd server
docker compose up -d

# 3. Run the backend (http://localhost:8080)
./mvnw spring-boot:run

# 4. In a new terminal, run the frontend (http://localhost:3000)
cd ../client
npm install
npm run dev
```

Detailed environment variables and configuration are in the [server README](server/README.md) and [client README](client/README.md).

## API documentation
With the backend running, open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). The raw OpenAPI spec is at `/v3/api-docs`.

## Roadmap
The core CRUD and booking flow is complete. Production hardening is in progress:
- [x] Swagger / OpenAPI documentation
- [ ] Dockerized backend (multi-stage Dockerfile) and full-stack Docker Compose
- [ ] Dual rotating JWT refresh tokens with reuse detection
- [ ] Redis caching for hot read paths
- [ ] Distributed rate limiting (Bucket4j)
- [ ] Live seat map over WebSocket/STOMP
- [ ] Automated tests: JUnit 5, Mockito, Testcontainers, JaCoCo coverage
- [ ] GitHub Actions CI
- [ ] k6 load test with recorded results
- [ ] Managed Postgres and Redis deployment, backend on Render

## Author
**Sourav Kumar**: [GitHub](https://github.com/asyncsourav) · [LinkedIn](https://www.linkedin.com/in/asyncsourav) · [LeetCode](https://leetcode.com/u/asyncsourav) · [Email](mailto:xsouravkumar357@gmail.com)
