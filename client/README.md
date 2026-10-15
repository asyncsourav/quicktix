# QuickTix Client

Production-grade Single Page Application (SPA) frontend for the QuickTix event ticket booking platform, built with [React 19](https://react.dev/), [Vite](https://vitejs.dev/), and [React Router 6](https://reactrouter.com/).

The client provides an interactive, responsive user experience for browsing events, visualizing real-time seat inventory, selecting seats dynamically, completing transactional bookings, and managing organizer/admin workflows.

**Live Deployment:** [https://quicktix-weld.vercel.app/](https://quicktix-weld.vercel.app/)

<!-- SCREENSHOT: QuickTix Client Interactive Event Detail Page with Seat Grid Picker and Checkout Sidebar -->

---

## Architectural Principles

1. **State Isolation**: Application state is separated into focused Context providers:
   * `AuthContext`: Manages authenticated user session, JWT persistence in `localStorage`, role resolution, and login/logout lifecycles.
   * `ThemeContext`: Controls theme state with smooth transitions and persistence across page reloads.
2. **Centralized HTTP Interceptor Pipeline**: All network requests pass through a configured [Axios](https://axios-http.com/) client instance that automatically attaches JWT Bearer authorization headers and intercepts HTTP 401 responses to purge stale credentials.
3. **Declarative Route Guards**: Route protection uses component-based guards (`ProtectedRoute`, `RoleRoute`) to prevent unauthorized access before components mount.
4. **Resilient Concurrency UI**: When a booking collision occurs (backend returns `409 Conflict`), the UI captures the error, displays an informative alert to the user, and immediately refreshes the seat map without losing surrounding page state.
5. **Zero CSS Framework Bloat**: Styled using pure Vanilla CSS with design tokens (HSL custom properties) for rapid layout calculation, custom scrollbars, and high-performance dark/light mode rendering.

---

## Technology Stack

* **Core Framework:** [React 19](https://react.dev/)
* **Build Tool & Bundler:** [Vite 6](https://vitejs.dev/)
* **Routing:** [React Router 6](https://reactrouter.com/)
* **HTTP Client:** [Axios 1.7](https://axios-http.com/)
* **Styling System:** Vanilla CSS 3 with CSS Custom Properties (Design Tokens)
* **Fonts:** [Inter](https://fonts.google.com/specimen/Inter) and [Outfit](https://fonts.google.com/specimen/Outfit) via Google Fonts
* **Deployment & CDN:** [Vercel](https://vercel.com/)

---

## Component & Application Architecture

```
client/src/
├── api/
│   └── client.js             # Axios instance with request and response interceptors
├── context/
│   ├── AuthContext.jsx       # Authentication state, login/logout, demo logins
│   └── ThemeContext.jsx      # Dark / Light mode toggle with localStorage sync
├── pages/
│   ├── HomePage.jsx          # Event discovery, category filters, and search bar
│   ├── EventDetailPage.jsx   # Interactive seat map grid, pricing summary, checkout
│   ├── MyBookingsPage.jsx    # User reservation history and ticket cancellation
│   ├── LoginPage.jsx         # Credentials form with 1-click Attendee/Organizer/Admin demo buttons
│   ├── RegisterPage.jsx      # New user registration form
│   ├── CreateEventPage.jsx   # Organizer portal for venue registration and event publication
│   └── AdminDashboard.jsx    # System metrics, user directory, and platform booking ledger
├── components/
│   ├── Navbar.jsx            # Responsive navigation, role-aware links, and theme toggle
│   ├── Footer.jsx            # Architectural footer with platform links
│   └── ProtectedRoute.jsx    # Role-based route guard wrapper
├── index.css                 # Master design system tokens, typography, and utility classes
├── App.jsx                   # Route definition and context provider hierarchy
└── main.jsx                  # Application entry point
```

---

## Key Features

### 1. Interactive Seat Reservation Grid
The seat map dynamically adapts to venue capacity (e.g., 50 seats for arenas, 40 for amphitheaters, 30 for conference halls):
* **Status Rendering**: Distinct visual treatments for `AVAILABLE` (selectable), `BOOKED` (disabled), and `SELECTED` (active selection).
* **Real-Time Price Calculation**: Displays seat count, itemized unit prices, and dynamic subtotal calculations.
* **Batch Reservation**: Users can select multiple seats across rows and submit them in a single transactional payload (`POST /api/bookings`).

<!-- SCREENSHOT: Seat Selection Grid showcasing Available, Selected, and Booked seat states alongside the order summary -->

### 2. Role-Based Access Control (RBAC)
The interface dynamically adjusts its navigation and views according to the authenticated user's role:
* **`USER` (Attendee):** Can browse events, inspect seat availability, book seats, view personal bookings, and cancel reservations.
* **`ORGANIZER`:** All attendee capabilities plus venue registration, event creation, and automatic seat inventory provisioning.
* **`ADMIN`:** Full access to all platform features, including the Admin Dashboard with system-wide booking ledgers, revenue aggregations, and user auditing.

<!-- SCREENSHOT: Admin Dashboard showcasing aggregate metrics, registered users table, and booking management view -->

### 3. One-Click Demo Mode
To simplify recruiter and reviewer evaluation, the login interface provides three pre-configured one-click demo login buttons:
* Attendee (`user@quicktix.com`)
* Organizer (`organizer@quicktix.com`)
* Administrator (`admin@quicktix.com`)

<!-- SCREENSHOT: Login page with 1-click quick login buttons for Attendee, Organizer, and Admin -->

---

## Page & Routing Registry

| Route Path | Component | Access Guard | Primary API Integrations |
| :--- | :--- | :---: | :--- |
| `/` | `HomePage` | Public | `GET /api/events` |
| `/events/:id` | `EventDetailPage` | Public | `GET /api/events/{id}`, `GET /api/events/{id}/seats`, `POST /api/bookings` |
| `/login` | `LoginPage` | Public (Guest) | `POST /api/auth/login` |
| `/register` | `RegisterPage` | Public (Guest) | `POST /api/auth/register` |
| `/bookings` | `MyBookingsPage` | Authenticated | `GET /api/bookings/me`, `DELETE /api/bookings/{id}` |
| `/create-event` | `CreateEventPage` | `ORGANIZER`, `ADMIN` | `GET /api/venues`, `POST /api/venues`, `POST /api/events` |
| `/admin` | `AdminDashboard` | `ADMIN` | `GET /api/admin/stats`, `GET /api/admin/users`, `GET /api/admin/bookings` |

---

## Local Development Setup

### Prerequisites
* [Node.js](https://nodejs.org/) (v18.0.0 or higher)
* [npm](https://www.npmjs.com/) (v9.0.0 or higher)
* Running QuickTix backend service on `http://localhost:8080` (see [server/README.md](../server/README.md))

### 1. Installation
```bash
cd client
npm install
```

### 2. Environment Configuration
Create a `.env` file in the `client/` root:
```bash
cp .env.example .env
```

Default configuration for local development:
```properties
# Local development API proxy base URL
VITE_API_BASE_URL=/api
```

*(Vite dev server proxies `/api` requests directly to `http://localhost:8080` via `vite.config.js` to eliminate CORS issues in local development).*

### 3. Start Development Server
```bash
npm run dev
```

The application will start at `http://localhost:5173` (or `http://localhost:3000`).

---

## Silent Token Refresh & Session Management

The frontend automatically manages token lifecycles through an Axios interceptor pipeline:
1. **Dual Storage**: Access tokens are held in `quicktix_token` and rotating refresh tokens in `quicktix_refresh_token`.
2. **Transparent 401 Interception**: When an access token expires (HTTP 401), outgoing network requests are paused and placed into an in-memory queue.
3. **Single Rotation Request**: The client dispatches a single call to `POST /api/auth/refresh` with the current refresh token.
4. **Queue Replay**: Once the backend returns a new access token and rotating refresh token, `localStorage` is updated, the default authorization header is refreshed, and all queued requests are re-executed without user interruption.
5. **Breach Handling**: If the refresh token is expired or flagged for reuse, the interceptor purges local session storage and redirects to `/login`.

---

## Production Build & Deployment

### Production Compilation
To generate an optimized, minified production build:
```bash
npm run build
```
Compiled static assets are generated in `client/dist/`.

To test the production build locally:
```bash
npm run preview
```

### Deployment Configuration (`vercel.json`)
Single Page Applications using client-side routing require rewrite rules so that deep links (e.g. `/events/1`, `/bookings`, `/admin`) resolve to `index.html`:

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

In the Vercel project configuration dashboard:
1. Set the root directory to `client`.
2. Configure environment variable: `VITE_API_BASE_URL=https://<your-backend-domain>/api`.
3. Deploy.

---

## Author

**Sourav Kumar**
* **GitHub:** [https://github.com/asyncsourav](https://github.com/asyncsourav)
* **LinkedIn:** [https://www.linkedin.com/in/asyncsourav](https://www.linkedin.com/in/asyncsourav)
* **LeetCode:** [https://leetcode.com/u/asyncsourav](https://leetcode.com/u/asyncsourav)
* **Email:** [xsouravkumar357@gmail.com](mailto:xsouravkumar357@gmail.com)
