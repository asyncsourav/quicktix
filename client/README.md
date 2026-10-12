# QuickTix Client

The React frontend for [QuickTix](../README.md), an event ticket booking platform. Users browse events, pick seats on an interactive seat map, and book them. Organizers and admins get the same app with extra permissions enforced by the backend.

**Live demo:** [https://quicktix-weld.vercel.app/](https://quicktix-weld.vercel.app/)

## Tech stack
- **React with Vite** for fast modern frontend development
- **Vanilla CSS & Design Tokens** with full Dark and Light mode toggle support
- **React Router (v6)** for client-side routing and protected routes
- **Axios** with request & response interceptors attaching JWT Bearer tokens and handling auto-logout on 401
- **React Context** (`AuthContext`, `ThemeContext`) for session and theme state persistence

## Pages
| Page | What it does | Backend calls |
|---|---|---|
| **Login / Register** | Forms with quick 1-click Demo Login for Attendee, Organizer, Admin | `POST /api/auth/login`, `POST /api/auth/register` |
| **Event List (Home)** | Lists events with category and search keyword filters | `GET /api/events` |
| **Event Detail + Seat Picker** | Renders real-time seat map grid, summary sidebar, and confirm flow | `GET /api/events/{id}`, `GET /api/events/{id}/seats`, `POST /api/bookings` |
| **My Bookings** | Lists user bookings with one-click seat cancellation | `GET /api/bookings/me`, `DELETE /api/bookings/{id}` |
| **Create Event** | Allows organizers to create venues and publish new events | `GET /api/venues`, `POST /api/venues`, `POST /api/events` |
| **Admin Dashboard** | Real-time platform revenue stats, registered users, and bookings table | `GET /api/admin/stats`, `GET /api/admin/users`, `GET /api/admin/bookings` |

If another user books a seat first, the booking request fails cleanly and the UI displays a clean notification that the seat was just taken, instead of crashing.

## Getting started
Prerequisites: Node.js 18+ and the backend running (see the [server README](../server/README.md)).

```bash
cd client
npm install
cp .env.example .env   # configured with VITE_API_BASE_URL
npm run dev
```

The app runs locally at `http://localhost:3000`.

## Environment variables
| Variable | Description | Default |
|---|---|---|
| `VITE_API_BASE_URL` | Base URL of the backend API | `/api` (proxied to `http://localhost:8080`) |

## Scripts
| Command | Description |
|---|---|
| `npm run dev` | Start the Vite dev server with hot module replacement |
| `npm run build` | Create an optimized production bundle in `dist/` |
| `npm run preview` | Preview the production build locally |

## Project structure
```
client/
├── src/
│   ├── api/          # Axios instance + JWT interceptor (client.js)
│   ├── context/      # AuthContext, ThemeContext
│   ├── pages/        # HomePage, EventDetailPage, MyBookingsPage, LoginPage, RegisterPage, CreateEventPage, AdminDashboard
│   ├── components/   # Navbar, Footer
│   ├── App.jsx       # Protected routes and providers
│   ├── index.css     # Design system, CSS tokens (Dark & Light), layout utilities
│   └── main.jsx
├── vercel.json       # SPA client-side routing rewrites for Vercel
├── index.html
└── vite.config.js
```

## Authentication flow
1. The user logs in; the backend returns a signed JWT.
2. `AuthContext` stores the token and user profile in `localStorage`.
3. The Axios interceptor adds `Authorization: Bearer <token>` to every subsequent API request.
4. The backend validates roles (`ADMIN`, `ORGANIZER`, `USER`) for protected actions.

## Deployment
The frontend is deployed on **Vercel**. In Vercel project settings, set `VITE_API_BASE_URL` to your live deployed backend URL (e.g. `https://your-backend.koyeb.app/api`).

## Roadmap
- [ ] Silent token refresh when access token expires (with backend rotating refresh tokens)
- [ ] Live seat map updates over WebSocket/STOMP with zero page refresh
