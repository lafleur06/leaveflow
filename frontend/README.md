# LeaveFlow frontend

The React client for the LeaveFlow API.

- **Everyone** logs in, sees their annual leave balance, requests and cancels leave, and sees the history of each request.
- **Managers** approve or reject their team's requests.
- **HR** also manages people and departments, roles and passwords, and public holidays. HR sees who is away in the coming weeks, downloads reports as Excel or CSV, and decides for people who have no manager.

React 19, TypeScript, Vite, React Router, TanStack Query. No UI library: one stylesheet (`src/index.css`) with design tokens.

## Run it

The API must be running on port 8080 (`./mvnw spring-boot:run` in the repository root).

```bash
npm install
npm run dev
```

Open <http://localhost:5173>. Vite proxies `/api` to `localhost:8080`, so the browser talks to a single origin and the API needs no CORS settings during development.

To work on the UI against the demo data instead, start the demo (`LEAVEFLOW_PORT=9090 docker compose -f compose.demo.yaml up --build` in the repository root) and point the proxy at it: `LEAVEFLOW_API=http://localhost:9090 npm run dev`.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Development server with hot reload |
| `npm run build` | Type-check (`tsc -b`) and production build into `dist/` |
| `npm run lint` | Lint with oxlint |

## How it is put together

- `src/api` – a typed `fetch` wrapper. It adds the bearer token, turns `problem+json` errors into `ApiError` (with per-field messages), and logs the user out on `401`.
- `src/auth` – the token lives in `sessionStorage`, so it survives a reload but not closing the tab. The current user comes from `GET /api/me`.
- `src/pages`:
  - login
  - "My leave": balance, request form, own requests with their history
  - "Approvals": the team's requests, plus, for HR, those of people without a manager
  - the HR pages: People, an employee's page, Public holidays, Reports
- HR pages are only linked and routed for HR. The API enforces the same rule, so hiding them is a convenience, not the protection.
- Reports are downloaded with `fetch`, because a plain link cannot carry the bearer token. The file name comes from the server's `Content-Disposition` header.
- Server state is cached and refreshed with TanStack Query; after a request, approval or cancellation the affected queries are invalidated instead of being patched by hand.
- The balance is drawn as one square per earned day (taken, waiting for approval, free); above 60 days one square stands for five.
- The login page asks `GET /api/demo/accounts` once. Only a server running the `demo` profile answers it. Everywhere else the call returns 404 and the page shows the normal login, so the same build works for the demo and for real use.

## In production

The root `Dockerfile` runs `npm run build` and copies `dist/` into the Spring Boot jar's static resources. The API then serves the client itself, from the same origin, exactly as the Vite proxy does during development.
