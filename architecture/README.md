# QuickTix Architecture & Engineering Index

Technical decisions, trade-offs, failure recovery patterns, and benchmarks behind QuickTix.

---

## Architectural Documentation Index

| Subsystem | Document Link | Core Architectural Decision |
| :--- | :--- | :--- |
| **Pipeline** | [`master-workflow.md`](master-workflow.md) | End-to-end request lifecycle and vertical sequence workflows for catalog, booking, and auth |
| **Concurrency** | [`concurrency-control.md`](concurrency-control.md) | JPA `@Version` optimistic locking preventing double-bookings with zero row locks; `race.mjs` test |
| **Caching** | [`distributed-caching.md`](distributed-caching.md) | Redis 7 distributed cache with composite keys, `@CacheEvict`, and fail-open `CacheErrorConfig` |
| **Rate Limiting** | [`distributed-rate-limiting.md`](distributed-rate-limiting.md) | Bucket4j + Lettuce anti-scalping shield on `POST /api/bookings` with dual IP/user tiers; `rate-limit.mjs` test |
| **Security** | [`security-and-auth.md`](security-and-auth.md) | In-memory access tokens, HttpOnly refresh cookies, SHA-256 DB hash, replay trap, and dynamic CORS |
| **Containers** | [`containerization.md`](containerization.md) | Multi-stage Dockerfile cutting image size by 50.1% (870 MB -> 434 MB); healthcheck orchestration |
| **Roadmap** | [`future-roadmap.md`](future-roadmap.md) | WebSocket/STOMP seat broadcasts via Redis Pub/Sub, Prometheus/Grafana, CI/CD, and k6 tests |

---

## Core Architectural Invariants

1. **PostgreSQL Enforces Correctness:** Business invariants (zero double-bookings) are guaranteed at the database layer via optimistic version predicates (`WHERE id = ? AND version = ?`).
2. **Auxiliary Layers Fail Open:** Redis caching and Bucket4j rate limiting fail open on timeouts or downtime. Auxiliary service interruptions never block ticket sales.
3. **Zero Trust in Browser Storage:** Access tokens stay strictly in JavaScript heap memory. Refresh tokens are single-use, rotated on every refresh, and isolated in Secure, HttpOnly cookies.
