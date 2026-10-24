# Security & Dual-Token Authentication Architecture

**Objective:** Stateless JWT authentication immune to XSS token theft, session hijacking, and refresh token replay attacks.

---

## Dual-Token Policy

| Token Type | Lifespan | Storage / Transmission | Security Purpose |
| :--- | :--- | :--- | :--- |
| **Access Token** | 15 Minutes | In-Memory (JavaScript heap) / `Authorization: Bearer <token>` | Stateless authentication; eliminated XSS persistence |
| **Refresh Token** | 7 Days | Secure, HttpOnly Cookie (`Path=/api/auth; SameSite=Lax`) | Inaccessible to `document.cookie`, preventing exfiltration |
| **Database Hash** | 7 Days | SHA-256 Digest in PostgreSQL (`refresh_tokens.token_hash`) | Database compromise cannot expose raw refresh tokens |

---

## Token Lifecycle & Replay Defense

```
[ POST /api/auth/login ] ────► Issues In-Memory Access Token + Sets HttpOnly Refresh Cookie
                                      │
[ POST /api/auth/refresh ] ──► Validates HttpOnly Cookie
                               ├── used == false: Atomic claim -> Issues new token pair (same family)
                               └── used == true:  Replay detected -> Revokes entire session family in DB -> 401
```

---

## Two Critical Production Bugs Solved

### 1. Transaction Rollback Bug on Reuse Detection
* **Problem:** `refresh()` was annotated with `@Transactional`. On token reuse detection, calling `revokeFamily()` followed by throwing `BadCredentialsException` caused Spring to roll back the entire transaction. The stolen session family remained valid in the database.
* **Fix in [`AuthService.java`](../server/src/main/java/com/asyncsourav/quicktix/service/AuthService.java):**
  ```java
  @Transactional(noRollbackFor = BadCredentialsException.class)
  public AuthResponse refresh(String incomingToken, HttpServletResponse response) { ... }
  ```
  The revocation commits even when `BadCredentialsException` is thrown.

### 2. Concurrency Race on Simultaneous Refreshes
* **Problem:** Two simultaneous refresh requests from the client could both read `used = false` before either committed, splintering the session into two active families.
* **Fix in [`RefreshTokenRepository.java`](../server/src/main/java/com/asyncsourav/quicktix/repository/RefreshTokenRepository.java):**
  ```java
  @Modifying
  @Query("UPDATE RefreshToken t SET t.used = true WHERE t.id = :id AND t.used = false AND t.revoked = false")
  int markAsUsed(@Param("id") Long id);
  ```
  Atomic statement: if `markAsUsed` returns `0`, the request aborts immediately.

---

## Dynamic CORS Configuration

Managed via [`CorsProperties.java`](../server/src/main/java/com/asyncsourav/quicktix/config/CorsProperties.java) and externalized to `application.yml`:

```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000,http://localhost:8080}
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
  allowed-headers: Authorization,Content-Type,X-Requested-With,Accept,Origin,Cookie,Set-Cookie
  exposed-headers: Authorization,Set-Cookie
  allow-credentials: true
```

Applied globally in [`SecurityConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/SecurityConfig.java) via `UrlBasedCorsConfigurationSource`, avoiding scattered `@CrossOrigin` annotations.
