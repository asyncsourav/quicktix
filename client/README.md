# QuickTix Client - React Single Page Application

The modern React frontend for QuickTix. Built with React 19 and Vite, it interfaces with the Spring Boot backend REST API and subscribes to STOMP WebSockets for real-time seat map synchronization.

- **Production Deployment:** https://quicktix-weld.vercel.app/
- **Live Backend API:** https://quicktix-api.onrender.com
- **Live Health Check:** https://quicktix-api.onrender.com/api/ping
- **Backend Architecture & Documentation:** [../server/README.md](../server/README.md)
- **Architecture Overview:** [../ARCHITECTURE.md](../ARCHITECTURE.md)

---

## Technical Highlights

- **Secure In-Memory Token Management:** Access tokens are stored exclusively in JavaScript memory and React context. No sensitive authentication tokens are written to `localStorage` or `sessionStorage`, protecting the application from Cross-Site Scripting (XSS) token extraction.
- **Silent Session Restoration:** When the application loads, an Axios interceptor dispatches a request to `/api/auth/refresh` with `withCredentials: true`. The browser securely sends the HttpOnly cookie, restoring the user session without user re-authentication or visual flashes.
- **Queued Refresh Interceptor:** If an API call fails with HTTP 401 Unauthorized, an Axios response interceptor queues pending requests, executes a single token refresh, and replays all queued calls with the new access token. This prevents duplicate refresh requests from triggering backend token reuse detection.
- **Real-Time Seat Picker:** Connects to the Spring STOMP broker at `/ws` using `@stomp/stompjs`. Subscribes to `/topic/event.{eventId}`, updating seat availability instantly when other users complete bookings.
- **Role-Based Protected Routes:** Enforces strict client-side access control with a reusable `ProtectedRoute` wrapper guarding routes for `USER`, `ORGANIZER`, and `ADMIN`.

---

## Application Structure

- `src/api/client.js`: Centralized Axios instance with in-memory token state and refresh interceptors.
- `src/context/AuthContext.jsx`: Authentication provider managing user profile, role state, and logout actions.
- `src/context/ThemeContext.jsx`: Theme provider supporting dark and light mode persistence.
- `src/components/Navbar.jsx`: Role-tailored navigation bar rendering exactly 3 links per user role.
- `src/pages/HomePage.jsx`: Minimalist landing page with hero search and featured events.
- `src/pages/EventsPage.jsx`: Filterable event catalog with synchronized URL search parameters.
- `src/pages/EventDetailPage.jsx`: Interactive seat map with live WebSocket status updates.
- `src/pages/MyBookingsPage.jsx`: Attendee booking history with ticket cancellation support.
- `src/pages/CreateEventPage.jsx`: Organizer portal for creating venues and publishing events.
- `src/pages/AdminDashboard.jsx`: Administrative dashboard with gross revenue and user metrics.

---

## Running Locally

### Prerequisites
- Node.js 18 or higher
- Running QuickTix backend service on `http://localhost:8080`

### Setup and Development
```bash
# 1. Install dependencies
npm install

# 2. Configure environment
cp .env.example .env
# Ensure VITE_API_BASE_URL=http://localhost:8080/api

# 3. Start development server
npm run dev
```
The application opens at `http://localhost:5173`.

### Production Build
```bash
# Compile and optimize production bundle into dist/
npm run build

# Preview production build locally
npm run preview
```

---

## Deployment (Vercel)

The frontend is deployed on Vercel with single-page app rewrites configured in `vercel.json`. For production deployments:
- Configure `VITE_API_BASE_URL` in Vercel environment variables pointing to the hosted backend.
- Ensure the backend's `CORS_ALLOWED_ORIGINS` includes the Vercel domain.
- Ensure the backend's `JWT_COOKIE_SECURE=true` is set so refresh cookies transmit across HTTPS domains (`SameSite=None; Secure`).
