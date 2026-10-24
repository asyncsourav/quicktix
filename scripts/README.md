# QuickTix Verification & Benchmark Scripts

I created this directory to house my standalone benchmark harnesses, concurrency verification runners, and cache invalidation test scripts. 

When building QuickTix, I didn't want to rely on theoretical assumptions or mocked unit tests alone. I wrote these automated Node.js scripts to fire live HTTP traffic against my running containers (`http://localhost:8080`) and physically verify how my backend behaves under real race conditions and heavy load.

---

## Why I Structured This as `scripts/` Instead of `resources/`

In enterprise Java and Spring Boot ecosystems, the folder name `resources` is strictly reserved for application classpath assets (`src/main/resources`). When I initially stored these scripts, I recognized that having a root-level `resources` directory created ambiguity with Maven packaging rules and IDE filters. 

To follow real-world production engineering standards, I organized all my benchmark tools, concurrency runners, and operational scripts under this clean `scripts/` directory at the project root.

---

## Prerequisites

To run these scripts on your machine:
* [Node.js 18+](https://nodejs.org/) installed.
* QuickTix backend running on `http://localhost:8080` (via `docker compose up -d` or `./mvnw spring-boot:run`).
* PostgreSQL 16 and Redis 7 active.

---

## The Scripts I Built

### 1. `race.mjs` - How I Verified Concurrency & Double-Booking Prevention

* **File:** `scripts/race.mjs`
* **Why I Built This:** During high-demand ticket drops, hundreds of users click the purchase button for the same seat at the exact same millisecond. I implemented JPA `@Version` optimistic locking on the `Seat` entity, and I built this script to physically prove that my database guarantees zero double-bookings under concurrent pressure.
* **How I Implemented the Test:**
  * I used `Promise.all` to dispatch 10 simultaneous asynchronous `POST /api/bookings` requests targeting the exact same seat (`eventId: 1, seatIds: [2]`) at the identical millisecond.
  * I formatted the responses into a clear terminal table to inspect HTTP status codes and error bodies side-by-side.
* **What I Found & Verified:**
  * Exactly 1 request successfully acquired the seat and returned **HTTP 201 Created**.
  * Exactly 9 requests detected the version conflict, rolled back atomically, and returned **HTTP 409 Conflict** ("This seat was just selected or booked by another user").
  * In PostgreSQL, exactly 1 booking record was created and the seat's `version` incremented from `1` to `2`.
* **How to Run:**
  ```bash
  node scripts/race.mjs
  ```

---

### 2. `latency.mjs` - How I Benchmarked Redis Read Performance

* **File:** `scripts/latency.mjs`
* **Why I Built This:** Browsing events accounts for over 90% of typical ticketing traffic. I implemented Redis 7 distributed caching via `@Cacheable` to shield PostgreSQL, and I wrote this benchmark script to quantify the exact latency reduction.
* **How I Implemented the Benchmark:**
  * The script automatically authenticates against `POST /api/auth/login` to retrieve a fresh JWT access token.
  * It runs 20 warmup requests to prime JVM JIT compilation and Redis memory.
  * It executes 200 sequential requests against both `GET /api/events` (catalog) and `GET /api/events/1` (detail view).
  * It calculates and displays average latency, p50 median, and p95 latency.
* **What I Measured:**
  * `GET /api/events` dropped from ~24.7ms on PostgreSQL to sub-18ms with Redis caching.
  * `GET /api/events/1` consistently resolved in ~11ms.
* **How to Run:**
  ```bash
  node scripts/latency.mjs
  ```

---

### 3. `check-eviction.mjs` - How I Verified Cache Invalidation (`@CacheEvict`)

* **File:** `scripts/check-eviction.mjs`
* **Why I Built This:** The biggest risk with caching is serving stale data. When an event organizer updates event details or prices, the old cached data in Redis must be purged immediately. I wrote this script to verify that my `@CacheEvict(value = {"events", "event"}, allEntries = true)` triggers properly upon modification.
* **How I Implemented the Test:**
  1. First, the script fetches `GET /api/events/1` to ensure the current version is warm in Redis.
  2. It authenticates as an organizer (`organizer@quicktix.com`).
  3. It sends a `PUT /api/events/1` request with a dynamically timestamped title.
  4. Immediately without pausing, it sends another `GET /api/events/1`.
  5. It compares the returned title with the sent update to confirm whether the stale cache was purged.
* **What I Verified:**
  * The script confirms "Eviction check passed: Fresh data returned immediately", proving that the cache key was purged and re-populated with fresh PostgreSQL data.
* **How to Run:**
  ```bash
  node scripts/check-eviction.mjs
  ```
