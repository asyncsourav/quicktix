# Security & Dual-Token Authentication Architecture

This document details how I designed, implemented, and hardened the authentication and session management architecture in QuickTix to protect against XSS token theft, session hijacking, and refresh token replay attacks.

---

## Why I Chose the Dual-Token Pattern

Early in development, I evaluated where to store session credentials. Storing JWTs in browser `localStorage` leaves tokens completely vulnerable to Cross-Site Scripting (XSS) exfiltration. Conversely, using server-side sessions creates statefulness that hurts horizontal scalability.

To get the best of both worlds, I implemented a **Dual-Token Pattern** paired with **Session Family Tracking** and **HttpOnly Cookies**:

| Token Type | Lifespan | Transmission / Storage | Why I Chose This Design |
| :--- | :--- | :--- | :--- |
| **Access Token** | 15 Minutes | In-Memory (JavaScript heap) / Authorization Bearer Header | Keeps authentication stateless while eliminating XSS persistence. |
| **Refresh Token** | 7 Days | Secure, HttpOnly Cookie (`Path=/api/auth; SameSite=Lax`) | Inaccessible to JavaScript `document.cookie`, preventing exfiltration. |
| **Database Hash** | 7 Days | SHA-256 Digest in PostgreSQL (`refresh_tokens.token_hash`) | If my database is ever compromised, raw tokens cannot be reversed. |

---

## The Refresh Token Lifecycle I Built

```
[ Login ] ────────► Issues In-Memory Access Token + Sets HttpOnly Refresh Cookie (Family UUID)
                           │
[ Rotation ] ─────► POST /api/auth/refresh (Browser transmits HttpOnly cookie automatically)
                    ├── Current token marked USED in PostgreSQL (atomic statement)
                    └── Issues new Access Token + Rotated HttpOnly Cookie (Same Family)
                           │
[ Replay Attack ] ─► Attacker presents already-used refresh token
                    ├── System detects REPLAY via SHA-256 hash lookup (used == true)
                    ├── Entire session family revoked in PostgreSQL
                    └── Cookie cleared (Max-Age=0), invalidating both attacker and victim
```

---

## Critical Bugs I Discovered & Solved During Implementation

While implementing the refresh token rotation in [`AuthService.java`](../server/src/main/java/com/asyncsourav/quicktix/service/AuthService.java), I caught and fixed two subtle vulnerabilities:

### 1. The Rollback Bug on Reuse Detection
* **The Bug:** `refresh()` was annotated with `@Transactional`. When token reuse was detected, the code called `revokeFamily(familyId)` and then threw `BadCredentialsException`. But because Spring transactions roll back by default on runtime exceptions, the entire revocation transaction was rolled back! The stolen session family remained valid in PostgreSQL.
* **How I Fixed It:** I explicitly configured the transaction boundary to commit the revocation even when throwing the exception:
  ```java
  @Transactional(noRollbackFor = BadCredentialsException.class)
  public AuthResponse refresh(String incomingToken, HttpServletResponse response) { ... }
  ```

### 2. The Concurrency Race Condition on Simultaneous Refreshes
* **The Bug:** If two simultaneous asynchronous requests from the frontend hit `/api/auth/refresh` with the same token, both threads could read `used == false` before either saved. Both would issue a new token, splintering the family into two active sessions.
* **How I Fixed It:** I turned token claiming into a single atomic database statement in [`RefreshTokenRepository.java`](../server/src/main/java/com/asyncsourav/quicktix/repository/RefreshTokenRepository.java):
  ```java
  @Modifying
  @Query("UPDATE RefreshToken t SET t.used = true WHERE t.id = :id AND t.used = false AND t.revoked = false")
  int markAsUsed(@Param("id") Long id);
  ```
  If `markAsUsed` returns `0`, another thread already claimed the token. The losing request immediately rejects and aborts, guaranteeing absolute session integrity.

---

## Centralized Dynamic CORS via YAML

To avoid scattering `@CrossOrigin` annotations with hardcoded URLs across controllers, I created [`CorsProperties.java`](../server/src/main/java/com/asyncsourav/quicktix/config/CorsProperties.java) and bound it directly to `application.yml`:

```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000,http://localhost:8080}
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
  allowed-headers: Authorization,Content-Type,X-Requested-With,Accept,Origin,Cookie,Set-Cookie
  exposed-headers: Authorization,Set-Cookie
  allow-credentials: true
```

In [`SecurityConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/SecurityConfig.java), I plugged `CorsProperties` into `UrlBasedCorsConfigurationSource`. This allows changing allowed domains seamlessly across local development, Docker, and cloud deployments via environment variables.
