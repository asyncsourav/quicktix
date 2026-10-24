# Security & Dual-Token Authentication Architecture

This document details the stateless JWT and rotating refresh token architecture used to secure user sessions and protect against XSS, token theft, and replay attacks.

---

## Token Architecture Overview

QuickTix implements a **Dual-Token Pattern** paired with **Session Family Tracking** and **HttpOnly Cookies**:

| Token Type | Lifespan | Transmission / Storage | Scope & Purpose |
| :--- | :--- | :--- | :--- |
| **Access Token** | 15 Minutes | In-Memory (JavaScript heap) / Authorization Bearer Header | Stateless authentication and role validation on every request |
| **Refresh Token** | 7 Days | Secure, HttpOnly Cookie (`Path=/api/auth; SameSite=Lax`) | Automated silent session rotation and long-lived sessions |
| **Database Hash** | 7 Days | SHA-256 Digest in PostgreSQL (`refresh_tokens.token_hash`) | Session auditing, rotation tracking, and replay detection |

---

## The Refresh Token Lifecycle

```
[ Login ] ────────► Issues In-Memory Access Token + Sets HttpOnly Refresh Cookie (Family #1)
                           │
[ Rotation ] ─────► POST /api/auth/refresh (Browser transmits HttpOnly cookie automatically)
                    ├── Current token marked USED in PostgreSQL (atomic statement)
                    └── Issues new Access Token + Rotated HttpOnly Cookie (Same Family #1)
                           │
[ Replay Attack ] ─► Attacker presents already-used refresh token
                    ├── System detects REPLAY via SHA-256 hash lookup (used == true)
                    ├── Entire session family revoked in PostgreSQL
                    └── Cookie cleared (Max-Age=0), invalidating both attacker and victim
```

---

## Defense Against Common Web Vulnerabilities

### 1. Cross-Site Scripting (XSS) Token Exfiltration
* **Vulnerability**: If JWT access tokens or refresh tokens are stored in `localStorage` or `sessionStorage`, any injected third-party JavaScript script can read `localStorage.getItem("token")` and exfiltrate credentials.
* **Defense**:
  * Access tokens are kept strictly in frontend memory (React context / JavaScript variables).
  * Refresh tokens are stored in `HttpOnly` cookies, which cannot be accessed by `document.cookie`.

### 2. Token Replay Attacks
* **Vulnerability**: If an attacker intercepts a refresh token, they can indefinitely generate fresh access tokens.
* **Defense**:
  * Every refresh request burns the submitted token (`used = true`).
  * If a token that was already burned is presented again, [`AuthService.java`](../server/src/main/java/com/asyncsourav/quicktix/service/AuthService.java) flags a reuse violation and immediately revokes all tokens sharing the same `family_id`.

### 3. Database Credential Leaks
* **Defense**: Refresh tokens are never stored in plaintext. When generated, only the **SHA-256 hash** is saved in PostgreSQL:
  ```java
  String tokenHash = DigestUtils.sha256Hex(rawToken);
  ```
  If the database table is dumped, raw refresh tokens cannot be reversed.

---

## Centralized Dynamic CORS

CORS is dynamically loaded from `application.yml` via [`CorsProperties.java`](../server/src/main/java/com/asyncsourav/quicktix/config/CorsProperties.java):
```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000,http://localhost:8080}
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
  allowed-headers: Authorization,Content-Type,X-Requested-With,Accept,Origin,Cookie,Set-Cookie
  exposed-headers: Authorization,Set-Cookie
  allow-credentials: true
```
This eliminates hardcoded frontend URLs and allows smooth deployment across staging, testing, and production environments.
