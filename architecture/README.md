# QuickTix Architecture & Engineering Handbook

I wrote this engineering handbook to document the technical decisions, trade-offs, failure recovery patterns, and benchmarks behind QuickTix. Rather than relying on boilerplate or surface-level code, I engineered each subsystem to solve the concrete distributed systems challenges that arise during high-concurrency ticket drops.

---

## Architecture Documents Index

| Document | What I Documented & Built |
| :--- | :--- |
| **[Master Request Lifecycle](master-workflow.md)** | My end-to-end trace from frontend click to database commit; sequence diagrams for all primary lifecycles (Catalog, Booking, Auth Rotation, Cache Eviction); failure mode resilience matrix. |
| **[Concurrency Control & Locking](concurrency-control.md)** | Why I chose optimistic locking with Hibernate `@Version` over pessimistic row locks; collision detection; atomic batch rollbacks; verification with `race.mjs`. |
| **[Distributed Caching](distributed-caching.md)** | How I configured Redis 7 distributed caching; solving stale reads with `@CacheEvict`; my `CacheErrorConfig` fail-open implementation; 1,000-event latency benchmarks. |
| **[Distributed Rate Limiting](distributed-rate-limiting.md)** | My Bucket4j + Lettuce token bucket anti-scalping shield on `POST /api/bookings`; dual-tier limits (Per-User burst 5, Per-IP burst 10); fail-open resilience; uniform HTTP 429 envelope. |
| **[Security & Dual-Token Auth](security-and-auth.md)** | My dual-token architecture; in-memory access tokens; Secure HttpOnly refresh cookies; SHA-256 database token hashing; two critical rollback/race bugs I solved. |
| **[Production Containerization](containerization.md)** | How I designed a multi-stage Docker build to cut container disk size by 50.1% (870 MB down to 434 MB); Docker Compose healthcheck-driven orchestration. |
| **[Future Architectural Roadmap](future-roadmap.md)** | The extensions I am designing next: WebSocket/STOMP real-time seat maps with Redis Pub/Sub, Prometheus/Grafana telemetry, CI/CD, and k6 stress testing. |

---

## My Core Architectural Principles

1. **PostgreSQL Guarantees Correctness:** I enforce business invariants (zero double-bookings) at the database layer using optimistic version predicates (`WHERE id = ? AND version = ?`).
2. **Auxiliary Layers Fail Open:** I built both Redis caching and Bucket4j rate limiting to fail open if Redis experiences an outage. An auxiliary service disruption must never take down ticket sales.
3. **Zero Trust in Browser Storage:** I never store access tokens or session credentials in `localStorage`. Access tokens remain strictly in frontend memory, while refresh tokens are single-use, rotated on every refresh, and isolated in Secure, HttpOnly cookies.
