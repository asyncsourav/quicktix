# QuickTix

An event ticketing platform. Users browse events, pick seats on a live seat map and book them. The backend guarantees a seat can never be booked twice, and every seat change shows up on other viewers' screens without a refresh.

**Live demo (frontend):** https://quicktix-weld.vercel.app/
**API docs (when running locally):** http://localhost:8080/swagger-ui.html

## What it does

- Register and log in as an attendee, organizer or admin
- Organizers create venues and publish events; seats are generated automatically from venue capacity
- Browse and filter events, open an event and see its seat map
- Book or cancel seats; the seat map updates live for everyone viewing that event
- Confirmation emails for bookings and cancellations
- Admin endpoints for platform stats, users and bookings

## Tech stack

| Area | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4, Spring Security, Spring Data JPA (Hibernate 7) |
| Database | PostgreSQL 16 |
| Cache and rate limiting | Redis 7, Spring Cache, Bucket4j (Lettuce) |
| Real-time | Spring WebSocket with STOMP and SockJS |
| Auth | JWT access tokens, rotating refresh tokens in HttpOnly cookies |
| API docs | springdoc-openapi (Swagger UI) |
| Frontend | React 19, Vite, React Router, Axios, @stomp/stompjs |
| Infrastructure | Docker, Docker Compose (multi-stage backend image) |

## Key design decisions

**Seats can't be double-booked.** Each seat has a `@Version` column. A booking runs in one transaction, and if two requests race for the same seat, one commit fails the version check, rolls back and returns `409 Conflict`. Correctness comes from PostgreSQL, not from anything in Redis.

**Auth without tokens in browser storage.** The access token (15 minutes) lives only in JavaScript memory. The refresh token (7 days) is an opaque value in an HttpOnly cookie, and only its SHA-256 hash is stored in the database. Every refresh is single-use and issues a new token in the same session family. If a used token shows up again, the whole family is revoked. Writing concurrent tests for this found two real bugs: the revoke was being rolled back by the exception thrown after it, and two simultaneous refreshes could both succeed. Both are fixed (`noRollbackFor`, and an atomic `UPDATE ... WHERE used = false`).

**Caching that fails open.** Event listing and event detail are cached in Redis with a 5-minute TTL and evicted when an event is created, updated or deleted. Seat availability is deliberately not cached, because it has to be accurate when someone books. If Redis is down, cache errors are logged and requests fall back to PostgreSQL.

**Rate limiting on booking.** `POST /api/bookings` is protected by per-user and per-IP token buckets stored in Redis, so the limit holds across several backend instances. Rejected requests get `429` with a `Retry-After` header. If Redis is unreachable the limiter fails open, since it protects the system but isn't what keeps bookings correct.

**Live seat map.** After a booking or cancellation commits, the backend publishes the seat change to `/topic/event.{id}` over STOMP, and the React seat picker subscribes to that topic.

**Email off the request path.** Confirmation emails are sent on a separate thread pool, so a slow mail server doesn't delay the booking response.

## Measured results

All numbers come from the scripts in [`scripts/`](scripts/), run against the Docker Compose stack on one local machine.

| What | Result |
|---|---|
| 10 simultaneous requests for the same seat | 1 × `201`, 9 × `409` |
| Event listing, 1,000 events, 200 sequential requests, p50 | 22.1 ms without cache, 17.7 ms with Redis (about 20% faster) |
| Single event, same setup, p50 | 15.3 ms without cache, 13.7 ms with Redis (about 10% faster) |
| 1,000 rapid-fire booking requests, 100 in flight | 994 blocked with `429` (99.4%) in 1.9 s, 6 allowed |
| Sustained 4 requests/second for 10 s | 11 allowed, about 1 per second |
| Live seat update to 50 WebSocket clients, 30 rounds | 1,500 of 1,500 delivered, median 15.8 ms, p95 20.7 ms |
| Backend Docker image | 870 MB (single-stage) to 434 MB (multi-stage), about 50% smaller |
| Automated test suite (JUnit 5 + Testcontainers) | 26 tests, 0 failures (JUnit 5, Mockito, Testcontainers for PostgreSQL & Redis) |
| JaCoCo test coverage | **43.2% line coverage** on service logic, **39.8%** overall (excludes boilerplate DTOs, entities, and configs) |

Notes: the cache gain is modest because PostgreSQL on localhost is already fast, and the WebSocket timer runs from the moment the booking request is sent to the moment each client receives the update. Both were measured on localhost, so a deployed setup will show different numbers.

## Run it locally

Requirements: Docker, and Node.js 18+ for the frontend.

```bash
git clone https://github.com/asyncsourav/quicktix.git
cd quicktix
cp .env.example .env
```

Set `JWT_SECRET` in `.env` to a long random value, for example:

```bash
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

Start PostgreSQL, Redis and the backend:

```bash
docker compose up -d --build
```

Start the frontend:

```bash
cd client
npm install
npm run dev
```

The app is at http://localhost:5173 and the API at http://localhost:8080. Demo data (users, venues, events) is seeded on first start.

To stop everything and keep the data: `docker compose down`. To also delete the database volume: `docker compose down -v`.

## Demo accounts

Seeded for local use only, all with the password `password123`:

| Role | Email |
|---|---|
| Admin | `admin@quicktix.com` |
| Organizer | `organizer@quicktix.com` |
| Attendee | `user@quicktix.com` |

## Repository layout

```
quicktix/
├── client/     React frontend          (see client/README.md)
├── server/     Spring Boot backend     (see server/README.md)
├── scripts/    Test and benchmark scripts for the results above
├── docker-compose.yml
└── .env.example
```

## Status

Working: everything above, including the 26-test automated suite (JUnit 5, Mockito, Testcontainers for PostgreSQL and Redis) with JaCoCo coverage reporting. Next: CI pipeline (GitHub Actions) and hosted production deployment.

## Author

Sourav Kumar · [GitHub](https://github.com/asyncsourav) · [LinkedIn](https://www.linkedin.com/in/asyncsourav) · [LeetCode](https://leetcode.com/u/asyncsourav)