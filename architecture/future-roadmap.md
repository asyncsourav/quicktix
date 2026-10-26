# Engineering Roadmap & Future Extensions

**Objective:** Near-term architectural extensions for real-time seat synchronization, centralized telemetry, automated CI/CD, and stress testing.

---

## 1. Real-Time Seat Availability (WebSocket / STOMP + Redis Pub/Sub)

Replace seat map manual page refreshes with full-duplex live seat status broadcasts:

```
[ Attendee A: POST /api/bookings ] ──► [ Spring Boot Pod 1 ]
                                              │
                                              ▼
                                   [ Redis Pub/Sub Channel ]
                                              │
                         ┌────────────────────┴────────────────────┐
                         ▼                                         ▼
                [ Spring Boot Pod 1 ]                     [ Spring Boot Pod 2 ]
                         │                                         │
                         ▼                                         ▼
                [ Attendee A WebSocket ]                  [ Attendee B WebSocket ]
                (Seat turns green: BOOKED)                (Seat turns gray: BOOKED)
```

* **Status:** In-memory WebSocket/STOMP broker and frontend subscription implemented (see [`realtime-seat-sync.md`](realtime-seat-sync.md)).
* **Next Extension (Clustered Redis Pub/Sub Broker):** When running multiple Spring Boot pods behind a load balancer, a booking on Pod 1 publishes to a Redis channel to fan out frames across all pods.
* **Handshake Security:** Validate existing Bearer JWT during the initial STOMP `CONNECT` frame.

---

## 2. Centralized Observability & Telemetry (Prometheus + Grafana)

* **Structured JSON Logging:** Emit structured logs with `traceId`, `userId`, `clientIp`, `path`, and execution duration.
* **Micrometer & Actuator Metrics Export:**
  * HikariCP connection pool active/idle counts and acquisition wait times.
  * Redis cache hit, miss, and timeout ratios.
  * HTTP 429 rate limit throttle rates by client IP and user.
  * HTTP 409 optimistic locking collision rates per event.
* **Grafana Dashboards:** Real-time visualization of p95/p99 latencies and system throughput.

---

## 3. Automated CI/CD Pipeline (GitHub Actions)

* **Stage 1 (Build & Unit Tests):** Maven compile under JDK 25 and execute JUnit 5 test suite.
* **Stage 2 (Integration Tests):** Spin up disposable PostgreSQL 16 and Redis 7 instances via Testcontainers to verify optimistic locking and rate limiting.
* **Stage 3 (Security & Containerization):** Multi-stage Docker image build paired with Trivy container vulnerability scanning.

---

## 4. Distributed High-Load Benchmarking (k6)

* **Scenario 1: Scalper Bot Flood:** 1,000 virtual users targeting `POST /api/bookings` concurrently. Target: 95%+ throttled with HTTP 429 within 200ms; zero DB connection exhaustion.
* **Scenario 2: Viral Ticket Drop Contention:** 500 concurrent requests competing for 50 available seats. Target: Exactly 50 bookings succeed (HTTP 201), 450 receive HTTP 409 Conflict, and zero double-bookings occur in PostgreSQL.
