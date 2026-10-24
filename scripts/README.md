# QuickTix Verification & Benchmark Scripts

This directory contains standalone benchmark harnesses, concurrency verification runners, and cache invalidation test scripts for the QuickTix platform.

---

## Directory Organization & Industry Standards

In production enterprise software (particularly Java and Spring Boot ecosystems):
* The name `resources` is strictly reserved for application classpath assets (`src/main/resources`). Creating a root-level `resources` directory causes confusion with Maven/Gradle packaging conventions and IDE resource filters.
* Operational utilities, load testing runners, and verification harnesses are standardly organized under `scripts/` (or `benchmarks/`).
* Scripts in this directory run against the live HTTP API (`http://localhost:8080`) to test the compiled application in its true containerized or runtime environment, rather than mocked unit contexts.

---

## Prerequisites

* [Node.js 18+](https://nodejs.org/) installed.
* QuickTix backend running on `http://localhost:8080` (either directly via `./mvnw spring-boot:run` or via `docker compose up -d`).
* PostgreSQL 16 and Redis 7 active.

---

## Script Catalog

### 1. `race.mjs` - Concurrency & Double-Booking Stress Test

* **File:** `scripts/race.mjs`
* **Purpose:** Verifies that Hibernate `@Version` optimistic locking prevents seat over-selling under simultaneous write pressure.
* **How It Works:**
  * Uses `Promise.all` to dispatch 10 parallel HTTP `POST /api/bookings` requests targeting the exact same seat (`eventId: 1, seatIds: [2]`) at the identical millisecond.
  * Formats all 10 responses into a clear terminal table showing HTTP status codes and response messages.
* **Expected Result:**
  * Exactly 1 request succeeds with **HTTP 201 Created**.
  * Exactly 9 requests are rejected with **HTTP 409 Conflict** ("This seat was just selected or booked by another user").
  * Zero double-bookings occur in PostgreSQL.
* **Execution:**
  ```bash
  node scripts/race.mjs
  ```

---

### 2. `latency.mjs` - Redis Read Performance Benchmark

* **File:** `scripts/latency.mjs`
* **Purpose:** Quantifies response time improvements gained from Redis 7 distributed caching on high-frequency read endpoints.
* **How It Works:**
  * Automatically authenticates against `POST /api/auth/login` to retrieve an access token.
  * Tests two primary catalog endpoints: `GET /api/events` and `GET /api/events/1`.
  * Executes 20 warmup requests to prime JVM JIT compilation and Redis caches.
  * Executes 200 sequential benchmark requests per endpoint.
  * Calculates and prints average latency, p50 median, and p95 latency.
* **Expected Result:**
  * Sub-20ms average and p50 response latencies when served directly from Redis memory.
* **Execution:**
  ```bash
  node scripts/latency.mjs
  ```

---

### 3. `check-eviction.mjs` - Cache Invalidation Verification

* **File:** `scripts/check-eviction.mjs`
* **Purpose:** Confirms that `@CacheEvict(value = {"events", "event"}, allEntries = true)` immediately invalidates Redis keys when an event is updated, preventing stale reads.
* **How It Works:**
  1. Performs `GET /api/events/1` to ensure the event is cached in Redis.
  2. Authenticates as an organizer (`organizer@quicktix.com`) to acquire an authorization token.
  3. Sends `PUT /api/events/1` with a dynamically generated event title.
  4. Immediately sends `GET /api/events/1` again without delay.
  5. Compares the returned title with the sent title to verify the stale cache was purged.
* **Expected Result:**
  * "Eviction check passed: Fresh data returned immediately."
* **Execution:**
  ```bash
  node scripts/check-eviction.mjs
  ```
