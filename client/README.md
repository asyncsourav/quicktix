# QuickTix Client

The Single Page Application (SPA) frontend for [QuickTix](../README.md), built with [React 19](https://react.dev/), [Vite 6](https://vitejs.dev/), and [React Router 6](https://reactrouter.com/).

The client provides an interactive venue seat selection experience, transparent session management with silent token refresh, and role-based views for Attendees, Organizers, and Administrators.

**Live Deployment:** [https://quicktix-weld.vercel.app/](https://quicktix-weld.vercel.app/)  
**Backend API Docs:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
**Architecture Deep Dives:** [../docs/architecture/](../docs/architecture/README.md)

---

## Architectural Highlights

* **State Isolation**: State is organized into focused React Context providers:
  * `AuthContext`: In-memory access token storage, user profile state, silent cookie-based session hydration on mount, and login/logout lifecycles. Zero credentials stored in `localStorage` to eliminate XSS token theft.
  * `ThemeContext`: Dark/Light theme state with CSS custom properties and persistence.
* **Silent Token Refresh Pipeline**: An [Axios](https://axios-http.com/) interceptor transparently intercepts HTTP 401 responses, queues concurrent in-flight requests, calls `/api/auth/refresh` with credentials (HttpOnly cookie), and replays requests with the new access token without interrupting the user.
* **Declarative Route Guards**: Component-level route guards (`ProtectedRoute`) prevent unauthorized navigation before views render, gracefully handling session verification loading states.
* **Resilient Concurrency UI**: When a booking collision occurs (backend returns HTTP 409 Conflict), the UI catches the error, displays an informative alert to the user, and immediately refreshes the seat map without losing surrounding page state.
* **Zero CSS Framework Overhead**: Built with pure Vanilla CSS and design tokens (custom CSS variables) for fast layout calculations, custom dark/light theme transitions, and zero style bundle bloat.

---

## Technology Stack

* **UI Framework:** [React 19](https://react.dev/)
* **Build Tool & Bundler:** [Vite 6](https://vitejs.dev/)
* **Client-Side Routing:** [React Router 6](https://reactrouter.com/)
* **HTTP Client:** [Axios 1.7](https://axios-http.com/)
* **Icons:** [lucide-react](https://lucide.dev/)
* **Styling System:** Vanilla CSS 3 with Custom Property Design Tokens
* **Typography:** Plus Jakarta Sans & JetBrains Mono via Google Fonts
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

### 3. Role-Based Access Control (RBAC)
* **Attendee (`USER`)**: Browse events, view seat availability, reserve seats, view and cancel personal bookings.
* **Organizer (`ORGANIZER`)**: All attendee capabilities plus venue creation, event publication, and seat inventory generation.
* **Administrator (`ADMIN`)**: Full platform oversight including user directories, platform revenue statistics, and booking ledgers.

---

## Routing & Page Registry

| Path | View Component | Access Tier | Primary Interactions |
| :--- | :--- | :---: | :--- |
| `/` | `HomePage` | Public | Live landing page, hero metrics, event catalog search |
| `/events/:id` | `EventDetailPage` | Public | Interactive seat map, seat selection, checkout |
| `/login` | `LoginPage` | Guest | Login form with 1-click demo role access |
| `/register` | `RegisterPage` | Guest | Account registration |
| `/my-bookings` | `MyBookingsPage` | Authenticated | View confirmed bookings, ticket cancellation |
| `/organizer/create` | `CreateEventPage` | `ORGANIZER`, `ADMIN` | Venue selection, event details, seat map generator |
| `/admin` | `AdminDashboard` | `ADMIN` | Platform metrics, user directories, booking audits |

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

---

## Author

**Sourav Kumar**
* **GitHub:** [https://github.com/asyncsourav](https://github.com/asyncsourav)
* **LinkedIn:** [https://www.linkedin.com/in/asyncsourav](https://www.linkedin.com/in/asyncsourav)
* **LeetCode:** [https://leetcode.com/u/asyncsourav](https://leetcode.com/u/asyncsourav)
* **Email:** [xsouravkumar357@gmail.com](mailto:xsouravkumar357@gmail.com)
