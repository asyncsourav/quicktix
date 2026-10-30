# QuickTix Project Navigation & File Directory

This document provides a complete directory map of the QuickTix codebase with direct links to navigate to every source file.

---

## 1. Server Architecture (Spring Boot Backend)

### Root Server Configuration
- [server/pom.xml](server/pom.xml) - Maven build descriptor containing Spring Boot 4, Java 25, Hibernate 7, Resilience4j, and Bucket4j dependencies.
- [server/Dockerfile](server/Dockerfile) - Multi-stage container build (Maven JDK build stage -> Alpine JRE 25 runtime stage).
- [server/.env.example](server/.env.example) - Template for required backend environment variables.
- [server/README.md](server/README.md) - Server overview and endpoint documentation.

### Core Application Entrypoint
- [QuicktixApplication.java](server/src/main/java/com/asyncsourav/quicktix/QuicktixApplication.java) - Main Spring Boot application class.

### Configuration (`server/src/main/java/.../config/`)
- [AsyncConfig.java](server/src/main/java/com/asyncsourav/quicktix/config/AsyncConfig.java) - Thread pool executor configuration for non-blocking asynchronous email dispatch.
- [CacheErrorConfig.java](server/src/main/java/com/asyncsourav/quicktix/config/CacheErrorConfig.java) - Graceful cache error handler ensuring queries fall back to PostgreSQL if Redis fails.
- [CorsProperties.java](server/src/main/java/com/asyncsourav/quicktix/config/CorsProperties.java) - Type-safe binding for cross-origin domain configurations.
- [DataInitializer.java](server/src/main/java/com/asyncsourav/quicktix/config/DataInitializer.java) - Startup seed runner generating demo venues, events, seats, and user accounts.
- [OpenApiConfig.java](server/src/main/java/com/asyncsourav/quicktix/config/OpenApiConfig.java) - Swagger UI and OpenAPI 3.0 specification metadata configuration.
- [RateLimiterConfig.java](server/src/main/java/com/asyncsourav/quicktix/config/RateLimiterConfig.java) - Lettuce Redis client bean initialization with authentication support.
- [RedisConfig.java](server/src/main/java/com/asyncsourav/quicktix/config/RedisConfig.java) - Spring Cache manager with JSON/string serialization and 5-minute TTL.
- [SecurityConfig.java](server/src/main/java/com/asyncsourav/quicktix/config/SecurityConfig.java) - Spring Security filter chain, CORS configuration, session policy, and route authorization rules.
- [WebSocketConfig.java](server/src/main/java/com/asyncsourav/quicktix/config/WebSocketConfig.java) - STOMP over WebSocket broker endpoint `/ws` and `/topic` destination prefix.

### Controllers (`server/src/main/java/.../controller/`)
- [AdminController.java](server/src/main/java/com/asyncsourav/quicktix/controller/AdminController.java) - Administrative endpoints for platform stats, bookings, and user records.
- [AuthController.java](server/src/main/java/com/asyncsourav/quicktix/controller/AuthController.java) - User registration, authentication, token refresh, and logout endpoints.
- [BookingController.java](server/src/main/java/com/asyncsourav/quicktix/controller/BookingController.java) - Seat reservation, user booking history, and cancellation endpoints.
- [EventController.java](server/src/main/java/com/asyncsourav/quicktix/controller/EventController.java) - Event catalog browsing, detail view, seat status, and event lifecycle endpoints.
- [PingController.java](server/src/main/java/com/asyncsourav/quicktix/controller/PingController.java) - Lightweight health-check endpoint for container orchestration.
- [VenueController.java](server/src/main/java/com/asyncsourav/quicktix/controller/VenueController.java) - Venue creation, lookup, and capacity management endpoints.

### Data Transfer Objects (`server/src/main/java/.../dto/`)
- [ApiResponse.java](server/src/main/java/com/asyncsourav/quicktix/dto/common/ApiResponse.java) - Generic envelope wrapper for standard API JSON responses.
- [ErrorResponse.java](server/src/main/java/com/asyncsourav/quicktix/dto/common/ErrorResponse.java) - Structured error payload containing status codes, messages, and timestamps.
- [BookingConfirmationEmailDetails.java](server/src/main/java/com/asyncsourav/quicktix/dto/email/BookingConfirmationEmailDetails.java) - Detached DTO for thread-safe asynchronous email templates.
- [BookingRequest.java](server/src/main/java/com/asyncsourav/quicktix/dto/request/BookingRequest.java) - Validated request payload containing target event and seat IDs.
- [EventCreateRequest.java](server/src/main/java/com/asyncsourav/quicktix/dto/request/EventCreateRequest.java) - Validated payload for event creation by organizers.
- [LoginRequest.java](server/src/main/java/com/asyncsourav/quicktix/dto/request/LoginRequest.java) - Credentials payload for user sign-in.
- [RefreshTokenRequest.java](server/src/main/java/com/asyncsourav/quicktix/dto/request/RefreshTokenRequest.java) - Fallback token refresh payload when cookies are unavailable.
- [RegisterRequest.java](server/src/main/java/com/asyncsourav/quicktix/dto/request/RegisterRequest.java) - Validated registration payload with role assignment.
- [VenueCreateRequest.java](server/src/main/java/com/asyncsourav/quicktix/dto/request/VenueCreateRequest.java) - Validated payload for registering new venues.
- [AdminStatsResponse.java](server/src/main/java/com/asyncsourav/quicktix/dto/response/AdminStatsResponse.java) - Aggregated revenue, user counts, and booking metrics DTO.
- [AuthResponse.java](server/src/main/java/com/asyncsourav/quicktix/dto/response/AuthResponse.java) - Access token, expiration, and user profile response DTO.
- [BookingResponse.java](server/src/main/java/com/asyncsourav/quicktix/dto/response/BookingResponse.java) - Confirmed booking response detailing seats and totals.
- [EventResponse.java](server/src/main/java/com/asyncsourav/quicktix/dto/response/EventResponse.java) - Cached event details DTO.
- [SeatResponse.java](server/src/main/java/com/asyncsourav/quicktix/dto/response/SeatResponse.java) - Individual seat status and price representation.
- [SeatStatusUpdate.java](server/src/main/java/com/asyncsourav/quicktix/dto/response/SeatStatusUpdate.java) - Real-time STOMP broadcast payload for seat state changes.
- [UserResponse.java](server/src/main/java/com/asyncsourav/quicktix/dto/response/UserResponse.java) - Public user profile representation.
- [VenueResponse.java](server/src/main/java/com/asyncsourav/quicktix/dto/response/VenueResponse.java) - Venue metadata and total capacity DTO.

### Persistence Entities & Enums (`server/src/main/java/.../entity/`)
- [Booking.java](server/src/main/java/com/asyncsourav/quicktix/entity/Booking.java) - JPA entity modeling customer reservations.
- [BookingStatus.java](server/src/main/java/com/asyncsourav/quicktix/entity/BookingStatus.java) - Enumeration of booking states (CONFIRMED, CANCELLED).
- [Event.java](server/src/main/java/com/asyncsourav/quicktix/entity/Event.java) - JPA entity modeling scheduled events and pricing.
- [RefreshToken.java](server/src/main/java/com/asyncsourav/quicktix/entity/RefreshToken.java) - JPA entity tracking SHA-256 token hashes, usage flags, and session family IDs.
- [Role.java](server/src/main/java/com/asyncsourav/quicktix/entity/Role.java) - Security authority enum (USER, ORGANIZER, ADMIN).
- [Seat.java](server/src/main/java/com/asyncsourav/quicktix/entity/Seat.java) - Seat entity featuring `@Version` optimistic locking to prevent race conditions.
- [SeatStatus.java](server/src/main/java/com/asyncsourav/quicktix/entity/SeatStatus.java) - Enumeration of seat inventory status (AVAILABLE, BOOKED).
- [User.java](server/src/main/java/com/asyncsourav/quicktix/entity/User.java) - JPA entity for registered accounts with password hashes and roles.
- [Venue.java](server/src/main/java/com/asyncsourav/quicktix/entity/Venue.java) - JPA entity representing physical hosting locations.

### Exception Handling (`server/src/main/java/.../exception/`)
- [GlobalExceptionHandler.java](server/src/main/java/com/asyncsourav/quicktix/exception/GlobalExceptionHandler.java) - Centralized `@RestControllerAdvice` translating exceptions into standard JSON errors.
- [BadRequestException.java](server/src/main/java/com/asyncsourav/quicktix/exception/BadRequestException.java) - Exception for malformed parameters (HTTP 400).
- [InvalidRoleException.java](server/src/main/java/com/asyncsourav/quicktix/exception/InvalidRoleException.java) - Exception for unauthorized role escalation.
- [RateLimitExceededException.java](server/src/main/java/com/asyncsourav/quicktix/exception/RateLimitExceededException.java) - Throttling exception carrying `Retry-After` header (HTTP 429).
- [ResourceNotFoundException.java](server/src/main/java/com/asyncsourav/quicktix/exception/ResourceNotFoundException.java) - Entity not found exception (HTTP 404).
- [SeatUnavailableException.java](server/src/main/java/com/asyncsourav/quicktix/exception/SeatUnavailableException.java) - Concurrency conflict exception during seat selection (HTTP 409).

### Repositories (`server/src/main/java/.../repository/`)
- [BookingRepository.java](server/src/main/java/com/asyncsourav/quicktix/repository/BookingRepository.java) - Spring Data JPA interface for booking queries and user history.
- [EventRepository.java](server/src/main/java/com/asyncsourav/quicktix/repository/EventRepository.java) - JPA queries with filtering and keyword search.
- [RefreshTokenRepository.java](server/src/main/java/com/asyncsourav/quicktix/repository/RefreshTokenRepository.java) - Atomic updates and token family revocation queries.
- [SeatRepository.java](server/src/main/java/com/asyncsourav/quicktix/repository/SeatRepository.java) - Seat lookup, batch status mutations, and event seat maps.
- [UserRepository.java](server/src/main/java/com/asyncsourav/quicktix/repository/UserRepository.java) - User lookup by unique email.
- [VenueRepository.java](server/src/main/java/com/asyncsourav/quicktix/repository/VenueRepository.java) - Venue retrieval and organizer ownership queries.

### Security (`server/src/main/java/.../security/`)
- [CustomUserDetailsService.java](server/src/main/java/com/asyncsourav/quicktix/security/CustomUserDetailsService.java) - Bridges Spring Security with `UserRepository`.
- [JwtAuthFilter.java](server/src/main/java/com/asyncsourav/quicktix/security/JwtAuthFilter.java) - Once-per-request servlet filter validating Bearer tokens and loading SecurityContext.
- [JwtUtil.java](server/src/main/java/com/asyncsourav/quicktix/security/JwtUtil.java) - HMAC-SHA256 token generation, validation, and claims parsing.

### Service Layer (`server/src/main/java/.../service/`)
- [AdminService.java](server/src/main/java/com/asyncsourav/quicktix/service/AdminService.java) - Business logic for administrative metrics and user lists.
- [AuthService.java](server/src/main/java/com/asyncsourav/quicktix/service/AuthService.java) - Authentication, token rotation, and reuse-detection family revocation.
- [BookingService.java](server/src/main/java/com/asyncsourav/quicktix/service/BookingService.java) - Transactional booking creation with optimistic locking and rollback safety.
- [EmailService.java](server/src/main/java/com/asyncsourav/quicktix/service/EmailService.java) - Asynchronous transactional confirmation email sender.
- [EventService.java](server/src/main/java/com/asyncsourav/quicktix/service/EventService.java) - Event publishing, automatic seat generation, and Redis cache-aside reads.
- [RateLimiterService.java](server/src/main/java/com/asyncsourav/quicktix/service/RateLimiterService.java) - Bucket4j Redis rate-limiting enforcement with Resilience4j circuit breaking.
- [SeatBroadcastService.java](server/src/main/java/com/asyncsourav/quicktix/service/SeatBroadcastService.java) - Real-time STOMP publisher pushing seat mutations to `/topic/event.{id}`.
- [VenueService.java](server/src/main/java/com/asyncsourav/quicktix/service/VenueService.java) - Venue registration and capacity calculations.

### Application Resources (`server/src/main/resources/`)
- [application.yml](server/src/main/resources/application.yml) - Base Spring Boot configuration (JPA, Jackson, OpenAPI, Mail).
- [application-dev.yml](server/src/main/resources/application-dev.yml) - Development profile targeting local Docker containers.
- [application-prod.yml](server/src/main/resources/application-prod.yml) - Production profile supporting Neon PostgreSQL SSL and cloud Redis.

### Automated Tests (`server/src/test/java/.../`)
- [BaseIntegrationTest.java](server/src/test/java/com/asyncsourav/quicktix/BaseIntegrationTest.java) - Abstract test base running PostgreSQL 16 and Redis 7 via Testcontainers.
- [BookingIntegrationTest.java](server/src/test/java/com/asyncsourav/quicktix/BookingIntegrationTest.java) - End-to-end integration tests for concurrency and booking workflows.
- [QuicktixApplicationTests.java](server/src/test/java/com/asyncsourav/quicktix/QuicktixApplicationTests.java) - Spring context verification test.
- [AdminControllerTest.java](server/src/test/java/com/asyncsourav/quicktix/controller/AdminControllerTest.java) - Controller unit tests for admin role security.
- [AuthControllerTest.java](server/src/test/java/com/asyncsourav/quicktix/controller/AuthControllerTest.java) - Controller unit tests for login and registration endpoints.
- [GlobalExceptionHandlerTest.java](server/src/test/java/com/asyncsourav/quicktix/exception/GlobalExceptionHandlerTest.java) - Tests validating structured HTTP error responses.
- [JwtUtilTest.java](server/src/test/java/com/asyncsourav/quicktix/security/JwtUtilTest.java) - Unit tests for token generation, expiration, and validation.
- [AuthServiceTest.java](server/src/test/java/com/asyncsourav/quicktix/service/AuthServiceTest.java) - Service tests for token rotation and reuse detection.
- [BookingServiceTest.java](server/src/test/java/com/asyncsourav/quicktix/service/BookingServiceTest.java) - Service tests for optimistic lock handling and seat validation.
- [EmailServiceTest.java](server/src/test/java/com/asyncsourav/quicktix/service/EmailServiceTest.java) - Tests verifying email template dispatch.
- [EventCacheTest.java](server/src/test/java/com/asyncsourav/quicktix/service/EventCacheTest.java) - Cache hit and eviction tests verifying Redis integration.
- [RateLimiterServiceTest.java](server/src/test/java/com/asyncsourav/quicktix/service/RateLimiterServiceTest.java) - Tests validating rate limit bursts and circuit breaker fail-open states.
- [SeatBroadcastServiceTest.java](server/src/test/java/com/asyncsourav/quicktix/service/SeatBroadcastServiceTest.java) - Tests verifying STOMP topic payloads.

---

## 2. Client Architecture (React Frontend)

### Root Client Configuration
- [client/package.json](client/package.json) - Frontend dependencies (React 19, Vite, Axios, StompJS, Lucide).
- [client/vite.config.js](client/vite.config.js) - Vite build configuration and dev server port setup.
- [client/vercel.json](client/vercel.json) - SPA routing configuration for Vercel deployment.
- [client/index.html](client/index.html) - Application HTML entry point.
- [client/.env.example](client/.env.example) - Template for frontend environment variables (`VITE_API_BASE_URL`).
- [client/README.md](client/README.md) - Client overview, authentication architecture, and build scripts.

### Entrypoint and Styles (`client/src/`)
- [client/src/main.jsx](client/src/main.jsx) - Application bootstrap mounting React DOM.
- [client/src/App.jsx](client/src/App.jsx) - Route definitions and role-based `ProtectedRoute` wrappers.
- [client/src/index.css](client/src/index.css) - Global CSS design tokens, dark/light themes, badges, buttons, and seat map styling.

### API & Network Services (`client/src/api/`)
- [client/src/api/client.js](client/src/api/client.js) - Central Axios client with in-memory token state, Bearer attachment, and queued 401 refresh retry logic.

### Context Providers (`client/src/context/`)
- [AuthContext.jsx](client/src/context/AuthContext.jsx) - Authentication state provider managing user profile, login, logout, and silent session restore.
- [ThemeContext.jsx](client/src/context/ThemeContext.jsx) - Theme state provider persisting dark/light mode preference.

### Shared UI Components (`client/src/components/`)
- [Navbar.jsx](client/src/components/Navbar.jsx) - Role-tailored navigation bar rendering exactly 3 purposeful links per role with active route indicators.
- [Footer.jsx](client/src/components/Footer.jsx) - Site-wide responsive footer with platform links and status links.

### Page Views (`client/src/pages/`)
- [HomePage.jsx](client/src/pages/HomePage.jsx) - Minimalist landing page with hero keyword/category search and featured event carousel.
- [EventsPage.jsx](client/src/pages/EventsPage.jsx) - Event discovery catalog with category filters, search input, and URL query parameter synchronization.
- [EventDetailPage.jsx](client/src/pages/EventDetailPage.jsx) - Interactive seat map with live WebSocket status updates and booking checkout modal.
- [MyBookingsPage.jsx](client/src/pages/MyBookingsPage.jsx) - Attendee booking dashboard with ticket cancellation support.
- [CreateEventPage.jsx](client/src/pages/CreateEventPage.jsx) - Organizer event publication form with automatic seat layout preview.
- [AdminDashboard.jsx](client/src/pages/AdminDashboard.jsx) - Administrative portal showing gross revenue, booking volumes, user accounts, and 1-click event creation.
- [LoginPage.jsx](client/src/pages/LoginPage.jsx) - Sign-in form supporting 1-click demo login for all roles.
- [RegisterPage.jsx](client/src/pages/RegisterPage.jsx) - User registration form supporting Attendee or Organizer account creation.

---

## 3. Verification & Benchmark Scripts (`scripts/`)

- [booking-load-test.js](scripts/booking-load-test.js) - k6 benchmark simulating 100 sustained virtual users against the booking service.
- [race.mjs](scripts/race.mjs) - Double-booking race condition test asserting optimistic locking under 10 concurrent requests.
- [latency.mjs](scripts/latency.mjs) - Redis cache performance harness comparing cached vs uncached event reads.
- [rate-limit.mjs](scripts/rate-limit.mjs) - Distributed rate-limiting verification script testing 1,000 rapid-fire bursts.
- [websocket-latency.mjs](scripts/websocket-latency.mjs) - STOMP WebSocket benchmark measuring broadcast delivery time across 50 connected clients.
- [check-eviction.mjs](scripts/check-eviction.mjs) - Cache-aside invalidation test verifying fresh data returns after updates.
- [scripts/README.md](scripts/README.md) - Script inventory, prerequisites, and execution commands.
