# QuickTix - High-Concurrency Event Ticketing Platform

[![Build](https://img.shields.io/github/actions/workflow/status/asyncsourav/quicktix/ci.yml?branch=main&logo=github)](https://github.com/asyncsourav/quicktix/actions/workflows/ci.yml)

A full-stack, distributed ticket booking system engineered to prevent race conditions during high-volume ticket drops. It guarantees that seats cannot be double-booked using JPA optimistic locking, synchronizes seat maps in real time via STOMP WebSockets, and protects resources using distributed token-bucket rate limiting backed by Redis.

- **Live Demo (Frontend):** https://quicktix-weld.vercel.app/
- **Live Backend API:** https://quicktix-api.onrender.com
- **Live Backend Health Check:** https://quicktix-api.onrender.com/api/ping
- **Live Swagger API Documentation:** https://quicktix-api.onrender.com/swagger-ui.html
- **Local Swagger (when running locally):** http://localhost:8080/swagger-ui.html
- **Architecture Deep-Dive:** [ARCHITECTURE.md](ARCHITECTURE.md)
- **Complete File Directory:** [folder-structure.md](folder-structure.md)

---

## Key Engineering Highlights

- **Zero Double-Booking Guarantee:** Seat reservation utilizes JPA optimistic locking (`@Version`). Concurrent booking attempts on the same seat trigger version conflicts, rolling back competing transactions and issuing HTTP 409 Conflict without pessimistic database deadlocks.
- **Stateless In-Memory Authentication:** JWT access tokens (15-minute lifespan) exist exclusively in client memory. Refresh tokens (7-day lifespan) are stored in secure HttpOnly cookies, persisted as SHA-256 hashes in the database with atomic reuse-detection and automatic session family revocation.
- **Fault-Tolerant Redis Caching:** Event catalog queries use Redis cache-aside with automated cache invalidation on writes. If Redis fails, a custom cache error handler catches the exception and falls back directly to PostgreSQL without failing user requests.
- **Distributed Rate Limiting with Circuit Breaker:** Bucket4j token buckets in Redis throttle booking attempts per IP and per user. A Resilience4j Circuit Breaker monitors Redis connectivity; if Redis goes down, the limiter fails open after 5 failed calls to protect core booking availability.
- **Real-Time Seat Synchronization:** Seat status mutations immediately publish to a Spring WebSocket STOMP broker on `/topic/event.{id}`, updating connected browser clients without requiring page reloads.
- **Off-Path Email Processing:** Confirmation emails execute asynchronously via a dedicated Spring thread pool, eliminating third-party SMTP latency from the HTTP response loop.

---

## Benchmark and Measured Metrics

All metrics were gathered from real traffic runs against the Dockerized stack:

| Evaluation Area | Target / Spec | Measured Result |
|---|---|---|
| **Redis Caching** | Response time of cached vs uncached event reads | Cut response time by ~75% (from 22.1 ms uncached down to 5.4 ms median / 11.0 ms p95 with Redis) |
| **Docker Multi-Stage Build** | Backend image footprint reduction | Reduced image size from 870 MB (build stage with JDK and Maven) to 457 MB / 148 MB runtime content size (48% reduction) |
| **Distributed Rate Limiting** | Bot mitigation under Bucket4j token bucket | Blocked 99.5% of rapid-fire attempts (995 of 1,000 requests in 1.66s) with HTTP 429 under a 5-burst user cap |
| **Real-Time Seat Broadcasting** | Concurrent STOMP subscribers latency | Propagated updates to 50 concurrent WebSocket clients in a median of 23.8 ms (p95 36.8 ms) across 1,500 deliveries (0 dropped messages) |
| **Automated Test Suite** | Test reliability and JaCoCo coverage | 39 automated tests passed (0 failures); JaCoCo reported 85% instruction coverage on controllers, 54% overall backend coverage |
| **Load Concurrency (k6)** | 100 sustained virtual users on booking service | Sustained 100 concurrent users at 48.9 ms p95 latency, 1,223.7 req/sec throughput, and 0.00% error rate (0 failed requests out of 61,406) |
| **Authentication Security** | Refresh token theft defense | Atomic single-use token rotation (`WHERE used = false`) with instant family revocation upon token reuse attempt |

---

## Tech Stack

- **Backend:** Java 25, Spring Boot 4, Spring Security, Spring Data JPA (Hibernate 7)
- **Database:** PostgreSQL 16
- **Caching & Throttling:** Redis 7, Bucket4j, Lettuce, Resilience4j
- **Real-Time Messaging:** Spring WebSocket, STOMP, SockJS
- **Frontend:** React 19, Vite, React Router, Axios, Lucide Icons, @stomp/stompjs
- **Testing:** JUnit 5, Mockito, Testcontainers (PostgreSQL & Redis), JaCoCo, k6
- **Infrastructure:** Docker, Docker Compose, GitHub Actions CI

---

## Repository Structure

- `server/`: Spring Boot backend service, configuration, entities, and integration tests. (See [server/README.md](server/README.md))
- `client/`: React 19 frontend application with role-based routing and responsive seat map. (See [client/README.md](client/README.md))
- `scripts/`: Production benchmark harnesses for race conditions, Redis latency, rate limiting, WebSockets, and k6 load tests. (See [scripts/README.md](scripts/README.md))
- `ARCHITECTURE.md`: Deep-dive technical documentation of the 10-stage request execution flow.
- `folder-structure.md`: Comprehensive file map with direct links to every source file across server and client.
- `docker-compose.yml`: Local orchestrator for backend, PostgreSQL, and Redis.

---

## Quick Start (Run Locally)

### Prerequisites
- Docker and Docker Compose
- Node.js 18+ (for running the frontend locally)

### 1. Clone Repository & Setup Environment
```bash
git clone https://github.com/asyncsourav/quicktix.git
cd quicktix
cp .env.example .env
```

### 2. Start Backend, PostgreSQL & Redis
```bash
docker compose up -d --build
```
The API becomes ready at `http://localhost:8080`. Demo users, venues, and events are automatically seeded on initial startup.

### 3. Start Frontend
```bash
cd client
npm install
npm run dev
```
The client application runs at `http://localhost:5173`.

### Demo User Accounts
All seeded accounts use password: `password123`
- **Admin:** `admin@quicktix.com` (Platform oversight, revenue analytics)
- **Organizer:** `organizer@quicktix.com` (Create venues, publish events)
- **Attendee:** `user@quicktix.com` (Browse events, reserve seats, view bookings)

---

## Running Verification & Benchmark Scripts

All verification scripts are located in the `scripts/` folder and can be run against the running backend:

- **k6 Load Test (100 concurrent users):** `k6 run scripts/booking-load-test.js`
- **Double-Booking Race Condition Test:** `node scripts/race.mjs`
- **Redis Cache Latency Benchmark:** `node scripts/latency.mjs`
- **Rate Limiting Enforcement Test:** `node scripts/rate-limit.mjs`
- **WebSocket Broadcast Latency Test:** `node scripts/websocket-latency.mjs`
- **Cache Eviction Invalidation Test:** `node scripts/check-eviction.mjs`

---

## Author

Sourav Kumar - [GitHub](https://github.com/asyncsourav) - [LinkedIn](https://www.linkedin.com/in/asyncsourav)
