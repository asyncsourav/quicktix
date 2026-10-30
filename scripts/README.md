# QuickTix Benchmark and Test Scripts

Automated scripts for verifying distributed system behavior under real concurrent traffic against the running Docker stack (backend, PostgreSQL 16, Redis 7).

- **Parent Repository:** [../README.md](../README.md)
- **Architecture Walkthrough:** [../ARCHITECTURE.md](../ARCHITECTURE.md)

---

## Scripts Inventory

| Script | Purpose | Evaluation Focus |
|---|---|---|
| `booking-load-test.js` | k6 load test simulating 100 sustained concurrent users | p95 latency, throughput (req/s), and error rate |
| `race.mjs` | Concurrent reservation attempts on the exact same seat | Double-booking prevention via JPA optimistic locking |
| `latency.mjs` | Compares cached vs uncached event catalog queries | Redis cache speedup and response latency |
| `rate-limit.mjs` | Rapid-fire booking requests under Bucket4j token bucket | Distributed rate-limiting and burst rejection (HTTP 429) |
| `websocket-latency.mjs` | Measures seat update propagation to 50 active clients | STOMP message delivery reliability and broadcast latency |
| `check-eviction.mjs` | Modifies event data and checks cache invalidation | Cache-aside consistency and eviction correctness |

---

## Prerequisites

- Node.js 18 or newer
- Running Docker Compose stack from the repository root: `docker compose up -d`
- k6 CLI installed (`winget install GrafanaLabs.k6` on Windows, or via Docker)

---

## How to Run Each Script

Run all scripts from the repository root:

### 1. Load Test (100 Concurrent Users)
Simulates 100 concurrent virtual users hitting the authenticated booking service:
```bash
# Using native k6:
k6 run scripts/booking-load-test.js

# Or using Docker:
Get-Content scripts/booking-load-test.js | docker run --rm -i --network quicktikbooking_default -e BASE_URL=http://quicktix-backend:8080 grafana/k6 run -
```
**Measured Result:** Sustained 100 concurrent users at 48.9 ms p95 latency, 1,223.7 req/sec throughput, and 0.00% error rate (0 failed requests out of 61,406).

### 2. Double-Booking Prevention Race Test
Fires 10 concurrent HTTP POST requests for the exact same seat simultaneously:
```bash
node scripts/race.mjs
```
**Measured Result:** Exactly 1 request succeeds with HTTP 201 Created; 9 requests fail with HTTP 409 Conflict. Zero double-bookings occur.

### 3. Redis Cache Latency Test
Sends 20 warm-up requests followed by 200 sequential queries to compare performance:
```bash
node scripts/latency.mjs
```
**Measured Result:** Cached reads return in 5.4 ms median (6.2 ms average, 11.0 ms p95) compared to 22.1 ms uncached (a ~75% reduction in query latency).

### 4. Distributed Rate Limiter Test
Executes 1,000 requests with 100 in flight, followed by a sustained 4 req/sec load:
```bash
node scripts/rate-limit.mjs
```
**Measured Result:** In Phase 1, 995 of 1,000 requests (99.5%) were blocked with HTTP 429 in 1.66s, adhering to the 5-token burst limit. In Phase 2, exactly 1.1 requests/sec were allowed, matching the token refill rate.

### 5. WebSocket Broadcast Latency Test
Connects 50 active STOMP WebSocket subscribers, alternates booking and releasing a seat for 30 rounds, and measures delivery time across 1,500 total messages:
```bash
node scripts/websocket-latency.mjs
```
**Measured Result:** 1,500 of 1,500 messages delivered (100% success rate, 0 lost messages) with a median latency of 23.8 ms and p95 latency of 36.8 ms.

### 6. Cache Invalidation Test
Updates event details and asserts that fresh data is immediately returned on the next read:
```bash
node scripts/check-eviction.mjs
```
**Measured Result:** Passed. Cache is instantly invalidated upon write; stale data is never served.
