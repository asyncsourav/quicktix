# QuickTix Client

The Single Page Application (SPA) frontend for [QuickTix](../README.md), built with [React 19](https://react.dev/), [Vite](https://vitejs.dev/), and [React Router 6](https://reactrouter.com/).

The client provides an interactive seat selection experience, transparent session management with silent token refresh, and role-based views for Attendees, Organizers, and Administrators.

**Live Deployment:** [https://quicktix-weld.vercel.app/](https://quicktix-weld.vercel.app/)

<!-- SCREENSHOT: QuickTix Client Interactive Event Detail Page with Seat Grid Picker and Checkout Sidebar -->

---

## Architectural Principles

1. **State Isolation**: State is organized into focused React Context providers:
   * `AuthContext`: In-memory access token state, user profile state, silent cookie-based session hydration on mount, and login/logout lifecycles. Zero credentials stored in `localStorage` to eliminate XSS token theft.
   * `ThemeContext`: Dark/Light theme state with CSS custom properties and persistence.
2. **Silent Token Refresh Pipeline**: An [Axios](https://axios-http.com/) interceptor transparently intercepts HTTP 401 responses, queues concurrent in-flight requests, calls `/api/auth/refresh` with credentials (HttpOnly cookie), and replays requests with the new access token without interrupting the user.
3. **Declarative Route Guards**: Component-level route guards (`ProtectedRoute`, `RoleRoute`) prevent unauthorized navigation before views render, gracefully handling session verification loading states.
4. **Resilient Concurrency UI**: When a booking collision occurs (backend returns HTTP 409 Conflict), the UI catches the error, displays an informative alert to the user, and immediately refreshes the seat map without losing surrounding page state.
5. **Zero CSS Framework Overhead**: Built with pure Vanilla CSS and design tokens (HSL variables) for fast layout calculations, custom dark/light theme transitions, and zero style bundle bloat.

---

## Technology Stack

* **UI Framework:** [React 19](https://react.dev/)
* **Build Tool & Bundler:** [Vite 6](https://vitejs.dev/)
* **Client-Side Routing:** [React Router 6](https://reactrouter.com/)
* **HTTP Client:** [Axios 1.7](https://axios-http.com/)
* **Icons:** [lucide-react](https://lucide.dev/)
* **Styling System:** Vanilla CSS 3 with Custom Property Design Tokens
* **Typography:** [Inter](https://fonts.google.com/specimen/Inter) and [Outfit](https://fonts.google.com/specimen/Outfit) via Google Fonts
* **Hosting & CDN:** [Vercel](https://vercel.com/) (Edge network with SPA client rewrites)

---

## Key Features

### 1. High-Performance Landing Page & Live Showcase
* **Hero Architecture Showcase**: Real-time platform metric counters (`0` double-bookings, `< 20ms` Redis read latency, `1,000+` live events, `100%` ACID isolation).
* **System Engineering Deep Dive**: Architectural feature cards detailing Hibernate `@Version` optimistic locking, Redis 7 distributed caching with automatic `@CacheEvict`, rotating HttpOnly refresh tokens, and real-time seat isolation.
* **3-Step Frictionless Workflow**: Visual user journey covering discovery, seat selection, and atomic checkout.
* **Live Interactive Catalog**: Category filters (`Concert`, `Sports`, `Theater`, `Conference`, `Comedy`), keyword search, and direct seat selection links.

### 2. Interactive Seat Reservation Grid
* **Dynamic Grid Layout**: Automatically adapts to venue capacity (e.g. 50 seats for arenas, 40 for amphitheaters, 30 for conference halls).
* **Seat State Rendering**: Clear visual states for `AVAILABLE` (selectable), `BOOKED` (disabled), and `SELECTED` (active user selection).
* **Live Price Calculation**: Calculates itemized pricing and subtotal dynamically as seats are toggled.
* **Atomic Multi-Seat Submission**: Submits batch seat selections to `POST /api/bookings` in a single transactional payload.

<!-- SCREENSHOT: Seat Selection Grid showcasing Available, Selected, and Booked seat states alongside the order summary -->

### 2. Silent Token Refresh with Request Queueing
When an access token expires (15 minutes), the Axios interceptor in [client.js](src/api/client.js):
1. Pauses outgoing API requests.
2. If multiple requests fail simultaneously, queues them in an in-memory queue (`failedQueue`) so only **one** `/api/auth/refresh` request is dispatched.
3. Calls `POST /api/auth/refresh` with `withCredentials: true`, allowing the browser to automatically attach the Secure, HttpOnly refresh cookie.
4. Once the refreshed access token arrives, updates in-memory authorization headers, and replays all queued requests seamlessly.
5. If refresh fails or token reuse is detected, clears in-memory credentials and redirects to `/login`.

### 3. Role-Based Access Control (RBAC)
* **Attendee (`USER`)**: Browse events, view seat availability, reserve seats, view and cancel personal bookings.
* **Organizer (`ORGANIZER`)**: All attendee capabilities plus venue creation, event publication, and seat inventory generation.
* **Administrator (`ADMIN`)**: Full platform oversight including user directories, platform revenue statistics, and booking ledgers.

<!-- SCREENSHOT: Admin Dashboard showcasing aggregate metrics, registered users table, and booking management view -->

### 4. One-Click Demo Mode
The sign-in interface includes pre-configured one-click demo login buttons for reviewers:
* Attendee (`user@quicktix.com`)
* Organizer (`organizer@quicktix.com`)
* Administrator (`admin@quicktix.com`)

<!-- SCREENSHOT: Login page with 1-click quick login buttons for Attendee, Organizer, and Admin -->

---

## Routing & Page Registry

| Path | View Component | Access Tier | API Interactions |
| :--- | :--- | :---: | :--- |
| `/` | `HomePage` | Public | `GET /api/events` |
| `/events/:id` | `EventDetailPage` | Public | `GET /api/events/{id}`, `GET /api/events/{id}/seats`, `POST /api/bookings` |
| `/login` | `LoginPage` | Guest | `POST /api/auth/login` |
| `/register` | `RegisterPage` | Guest | `POST /api/auth/register` |
| `/my-bookings` | `MyBookingsPage` | Authenticated | `GET /api/bookings/me`, `DELETE /api/bookings/{id}` |
| `/organizer/create` | `CreateEventPage` | `ORGANIZER`, `ADMIN` | `GET /api/venues`, `POST /api/venues`, `POST /api/events` |
| `/admin` | `AdminDashboard` | `ADMIN` | `GET /api/admin/stats`, `GET /api/admin/users`, `GET /api/admin/bookings` |

---

## Local Development Setup

### Prerequisites
* [Node.js 18+](https://nodejs.org/)
* QuickTix backend running on `http://localhost:8080` (see [server/README.md](../server/README.md))

### 1. Installation
```bash
cd client
npm install
```

### 2. Environment Configuration
```bash
cp .env.example .env
```
Default `.env` configuration:
```properties
VITE_API_BASE_URL=/api
```
*(Vite proxies `/api` requests directly to `http://localhost:8080` via `vite.config.js` to eliminate local CORS friction).*

### 3. Run Development Server
```bash
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## Production Build & Deployment

### Build Command
```bash
npm run build
```
Generates minified, tree-shaken static assets in `client/dist/`.

### Deployment on Vercel
Single Page Applications require routing rewrites so that deep links resolve to `index.html`. QuickTix includes [vercel.json](vercel.json):
```json
{
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

In the Vercel dashboard:
1. Set Framework Preset to **Vite**.
2. Set Root Directory to **`client`**.
3. Set Environment Variable: `VITE_API_BASE_URL=https://<your-backend-domain>/api`.

---

## Author

**Sourav Kumar**
* **GitHub:** [https://github.com/asyncsourav](https://github.com/asyncsourav)
* **LinkedIn:** [https://www.linkedin.com/in/asyncsourav](https://www.linkedin.com/in/asyncsourav)
* **LeetCode:** [https://leetcode.com/u/asyncsourav](https://leetcode.com/u/asyncsourav)
* **Email:** [xsouravkumar357@gmail.com](mailto:xsouravkumar357@gmail.com)
