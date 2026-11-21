# LeaveFlow frontend

React client for the LeaveFlow API. Built with React, TypeScript, Vite, React Router and TanStack Query. All styles are in `src/index.css`.

## Running

The backend has to run on port 8080 first.

```bash
npm install
npm run dev
```

Open http://localhost:5173. Vite forwards `/api` requests to the backend, so no CORS setup is needed.

To use the demo container on another port instead: `LEAVEFLOW_API=http://localhost:9090 npm run dev`.

Other commands: `npm run build`, `npm run lint`.

## Structure

- `src/api`: fetch wrapper that adds the token and turns error responses into `ApiError`
- `src/auth`: login state; the token is kept in `sessionStorage`
- `src/pages`: My leave, Approvals, and the HR pages (People, Public holidays, Reports)
- `src/components`: layout, balance strip, request history and small UI parts

HR pages are only shown to HR users, but the real check is done by the API.

In the Docker image the built files are copied into the Spring Boot jar, so the backend serves the frontend too.
