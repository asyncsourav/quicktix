# QuickTix Architecture & Engineering Handbook

Welcome to the QuickTix technical documentation directory. This repository contains detailed architectural specifications, failure mode analysis, benchmark methodologies, and end-to-end execution traces for the platform.

---

## Architecture Documents Index

| Document | Key Topics Covered |
| :--- | :--- |
| **[Master Request Lifecycle](master-workflow.md)** | Complete end-to-end trace from frontend click to database commit; Mermaid sequence diagrams for all primary lifecycles (Catalog, Booking, Auth Rotation, Cache Eviction); Failure modes & resilience matrix. |
| **[Concurrency Control & Locking](concurrency-control.md)** | Optimistic concurrency control via Hibernate `@Version`; Collision detection; Atomic batch rollbacks; Comparison with pessimistic locking. |
| **[Distributed Caching](distributed-caching.md)** | Redis 7 integration; Spring Cache `@Cacheable` and `@CacheEvict` policies; 5-minute safety TTL; Fail-open error handling with `CacheErrorConfig`; 1,000-event latency benchmarks. |
| **[Distributed Rate Limiting](distributed-rate-limiting.md)** | Bucket4j + Lettuce + Redis 7 implementation; Anti-scalping bot threat model; Dual-tier limits (Per-User burst 5, Per-IP burst 10); Fail-open pattern; HTTP 429 response envelope. |
| **[Security & Dual-Token Auth](security-and-auth.md)** | In-memory 15-minute access tokens; Secure HttpOnly 7-day refresh cookies; SHA-256 token hashing; Session family tracking and reuse detection; Centralized YAML CORS. |
| **[Production Containerization](containerization.md)** | Multi-stage Docker build pipeline; 50.1% container size reduction (870 MB to 434 MB); Docker Compose multi-service orchestration with healthchecks. |
| **[Future Architectural Roadmap](future-roadmap.md)** | WebSocket/STOMP live seat broadcast design; Centralized ELK observability; GitHub Actions CI/CD pipeline; k6 load testing scenarios. |

---

## Quick Reference: Core Architectural Principles

1. **PostgreSQL Guarantees Correctness**: Business invariants (zero double-bookings) are enforced at the database level via optimistic version predicates (`WHERE id = ? AND version = ?`).
2. **Caches & Limiters Fail Open**: Protection and optimization layers (Redis caching and Bucket4j rate limiting) fail open if Redis is down. An outage of an auxiliary service never takes down core ticket booking.
3. **Stateless Security**: Zero access tokens or sensitive credentials are stored in browser `localStorage`. Refresh tokens are single-use, rotated on every refresh, and tracked in database session families.
