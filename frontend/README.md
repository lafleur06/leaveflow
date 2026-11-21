# LeaveFlow frontend

A small React client for the LeaveFlow API: log in, see your annual leave balance, request leave, cancel it, and approve or reject your team's requests.

React 19, TypeScript, Vite, React Router, TanStack Query. No UI library: one stylesheet (`src/index.css`) with design tokens.

## Run it

The API must be running on port 8080 (`./mvnw spring-boot:run` in the repository root).

```bash
npm install
npm run dev
```

Open <http://localhost:5173>. Vite proxies `/api` to `localhost:8080`, so the browser talks to a single origin and the API needs no CORS settings during development.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Development server with hot reload |
| `npm run build` | Type-check (`tsc -b`) and production build into `dist/` |
| `npm run lint` | Lint with oxlint |

## How it is put together

- `src/api` – a typed `fetch` wrapper. It adds the bearer token, turns `problem+json` errors into `ApiError` (with per-field messages), and logs the user out on `401`.
- `src/auth` – the token lives in `sessionStorage`, so it survives a reload but not closing the tab. The current user comes from `GET /api/me`.
- `src/pages` – login, "My leave" (balance, request form, own requests) and "Team approvals".
- Server state is cached and refreshed with TanStack Query; after a request, approval or cancellation the affected queries are invalidated instead of being patched by hand.
- The balance is drawn as one square per earned day (taken, waiting for approval, free); above 60 days one square stands for five.
