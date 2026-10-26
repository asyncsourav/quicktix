# QuickTix Scripts

Small Node.js scripts that send real traffic to the running backend to check behaviour that unit tests with mocks can't show: races between concurrent requests, cache speed-up and eviction, rate limiting, and live WebSocket updates.

All results below were measured on one local machine against the Docker Compose stack (backend, PostgreSQL 16, Redis 7). Treat them as local numbers, not production numbers.

| Script | What it checks |
|---|---|
| `race.mjs` | A seat can't be booked twice under simultaneous requests |
| `latency.mjs` | Response time of cached vs uncached event reads |
| `check-eviction.mjs` | The cache is cleared when an event is updated |
| `rate-limit.mjs` | Booking requests are throttled with `429` |
| `websocket-latency.mjs` | How fast a seat change reaches connected clients |

## Setup

- Node.js 18 or newer
- The stack running: `docker compose up -d --build` from the repository root
- Seeded demo data (loaded on first start). The scripts log in with the demo accounts from the root README
- For `websocket-latency.mjs`: `cd scripts && npm install` (it needs `@stomp/stompjs` and `ws`)

Run any script from the repository root, for example `node scripts/race.mjs`. Edit the constants at the top of a script (event ID, seat ID, token or credentials) if your data differs. Resetting everything, including the seeded data: `docker compose down -v` and then `docker compose up -d`.

---

## `race.mjs`: double-booking check

Sends 10 `POST /api/bookings` requests for the same seat at the same moment using `Promise.all`, and prints the status codes.

Expected: exactly one `201 Created`, and the rest rejected. The seat must be `AVAILABLE` before the run, so use a fresh seat or cancel the previous booking.

Recorded result (before the rate limiter was added): 1 × `201`, 9 × `409` ("This seat was just selected or booked by another user").

Booking is now rate limited per user (burst of 5), so a single-user run also shows some `429` responses. The guarantee being tested still shows: only one request gets a `201`.

---

## `latency.mjs`: Redis cache speed

Logs in, sends 20 warm-up requests, then 200 sequential requests each to `GET /api/events` and `GET /api/events/1`, and prints average, p50 and p95 in milliseconds.

To compare, run it twice against the same data:

1. Cache off: add `SPRING_CACHE_TYPE: none` to the backend's `environment` in `docker-compose.yml`, run `docker compose up -d`, then run the script.
2. Cache on: remove that line, run `docker compose up -d` again, then run the script.

Dataset: 1,000 events (4 seeded plus 996 generated with a SQL insert).

| Endpoint | Metric | No cache | Redis cache |
|---|---|---|---|
| `GET /api/events` | p50 | 22.1 ms | 17.7 ms |
| | average | 24.7 ms | 18.4 ms |
| | p95 | 38.2 ms | 29.2 ms |
| `GET /api/events/1` | p50 | 15.3 ms | 13.7 ms |
| | average | 15.2 ms | 14.1 ms |
| | p95 | 25.1 ms | 22.5 ms |

The gain is modest because PostgreSQL on localhost is already fast and part of every request is spent in the security filter and JSON serialization. A remote database or concurrent load would show a larger difference.

---

## `check-eviction.mjs`: cache invalidation

Reads `GET /api/events/1` so it is cached, logs in as the organizer, updates the event with `PUT /api/events/1` using a timestamped title, then reads the event again and compares titles.

Expected output: `Eviction check passed: Fresh data returned immediately.` A failure means a stale cached value was served after the update.

---

## `rate-limit.mjs`: booking throttling

Uses a dedicated test user so it doesn't affect other tests' limits, and books a seat that is already taken so no data changes. Any request that gets past the limiter is answered by the booking logic, and anything else is `429`.

- Phase 1: 1,000 requests, 100 in flight at a time
- Phase 2: 4 requests per second for 10 seconds

Limits in the backend: per user, burst 5 and refill 1 token per second; per IP, burst 10 and refill 2 tokens per second.

| Run | Result |
|---|---|
| Phase 1 | 994 of 1,000 blocked with `429` (99.4%) in 1.9 s; 6 allowed (the burst plus one refill) |
| Phase 2 | 11 allowed over 10 s, about 1.1 per second |

A `429` response carries `Retry-After: 1` and the standard JSON error body. The token buckets live in Redis, so wait 10 to 15 seconds between runs or the first numbers will look wrong.

---

## `websocket-latency.mjs`: live seat updates

Connects 50 STOMP clients to `ws://localhost:8080/ws`, all subscribed to `/topic/event.1`. It then runs 30 rounds that alternate booking and cancelling a seat, and records when each client receives the update. Pass a different client count as an argument, for example `node scripts/websocket-latency.mjs 10`.

The timer starts right before the booking or cancellation request is sent, so each time includes the HTTP request, the database transaction and the broadcast.

| Metric | Result |
|---|---|
| Delivery | 1,500 of 1,500 messages received (50 clients × 30 rounds) |
| Median | 15.8 ms |
| p95 | 20.7 ms |
| Min / max | 9.7 ms / 39.8 ms |

All 50 clients run in one Node process on the same machine as the server, so this excludes real network latency and browser rendering. A deployed setup will be slower.