# QuickTix Client

The React frontend for [QuickTix](../README.md). It talks to the Spring Boot API in [`../server`](../server/README.md) and listens to its WebSocket endpoint for live seat updates.

**Live demo:** https://quicktix-weld.vercel.app/

## Stack

React 19, Vite, React Router, Axios, @stomp/stompjs.

## What's in it

- Landing page, login and registration
- Event list with filtering and search
- Event page with a seat picker. Seats change state live when anyone else books or cancels, without a refresh
- My bookings, with cancellation
- Route guards for the three roles (`USER`, `ORGANIZER`, `ADMIN`)

## Authentication on the client

- The access token is kept in memory only (a module variable plus React state). Nothing is written to `localStorage` or `sessionStorage`.
- The refresh token is an HttpOnly cookie that JavaScript cannot read. The Axios instance is created with `withCredentials: true` so the browser sends it to `/api/auth/refresh`.
- When the app loads, `AuthContext` calls `/api/auth/refresh` once to restore the session. `ProtectedRoute` waits for that check to finish, so a page reload doesn't flash the login page.
- A response interceptor handles `401` by refreshing the token and retrying the request. Refresh requests are queued, so several failed requests trigger one refresh, not many. This matters because the backend treats a reused refresh token as theft and ends the session.

## Run it

Requirements: Node.js 18+ and the backend running on http://localhost:8080 (see the [root README](../README.md)).

```bash
cd client
npm install
cp .env.example .env     # set the backend URL, variable names are in .env.example
npm run dev
```

The app runs at http://localhost:5173. The backend must list that origin in `CORS_ALLOWED_ORIGINS`.

| Command | What it does |
|---|---|
| `npm run dev` | Dev server with hot reload |
| `npm run build` | Production build into `dist/` |
| `npm run preview` | Serve the production build locally |

## Project layout

```
client/
├── src/
│   ├── api/client.js            Axios instance, in-memory token, refresh interceptor
│   ├── context/AuthContext.jsx  Current user, login, logout, session restore
│   ├── App.jsx                  Routes and ProtectedRoute
│   └── ...                      Pages and components
├── .env.example
├── vercel.json
└── vite.config.js
```

## Deployment

The frontend is deployed on Vercel (`vercel.json` is in this folder). Set the backend URL in the Vercel project's environment variables. For a deployed setup, the backend's `CORS_ALLOWED_ORIGINS` must include the Vercel URL, and because the frontend and backend are on different sites, the refresh cookie needs `Secure` and `SameSite=None`.

## Still to do

Automated frontend tests.