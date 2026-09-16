# Frontend

React + TypeScript SPA with Vite, Tailwind CSS, React Router and Axios. M1 includes a responsive development home, live health status/retry and a not-found route. M3 adds login, registration, auth state and role-aware account routes. Full domain dashboards remain in M4.

## Run

Use Node.js 22.12+ (Node 24 LTS recommended) and npm. From `frontend/`:

```bash
npm ci
cp .env.example .env
npm run dev
```

Open **http://localhost:5173**. Start the backend separately; its default dev profile needs no MySQL. Vite uses a strict port so a busy port cannot silently break CORS. Use `localhost` in the browser; `127.0.0.1` is a different origin and must be explicitly added to backend CORS if needed.

`VITE_API_BASE_URL` is a public API base including `/api`; it defaults to `http://localhost:8080/api`. Vite loads `frontend/.env`; restart after changing it. Production values are embedded at build time. Never put credentials or JWT secrets in `VITE_*`.

## Checks

```bash
npm run lint
npm run build
npm run preview
```

Lint uses the current Vite template's Oxlint with React/hooks and TypeScript rules; warnings fail the check. The build performs strict TypeScript checking before bundling. Preview uses port 5173; stop the dev server first. A future static host must fall back to `index.html` for SPA routes.

## Source map

- `src/app/`: application entry and Tailwind design tokens/styles.
- `src/router/` and `src/layouts/`: shared routing and page shell.
- `src/pages/`: home and not-found pages.
- `src/components/ConnectionCard.tsx`: loading, Connected, Unavailable and retry states.
- `src/services/api.ts`: central Axios client, eight-second timeout and environment-driven base URL.
- `src/services/health.ts`, `src/types/health.ts`: typed and runtime-checked health contract; requests cancel on cleanup.

Verified stable versions: react 19.3.0, typescript 7.0.2, vite 8.3.0, tailwindcss 4.3.3, react-router 8.3.1, axios 1.20.0.

Versions are pinned reproducibly in `package-lock.json`. The setup follows the official [Vite guide](https://vite.dev/guide/) and [Tailwind Vite integration](https://tailwindcss.com/docs).

## Authentication

Open `/register` to create a TECH/TRADE candidate or employer, or `/login` to sign in. Successful authentication routes to `/{role}/dashboard`, currently an account summary. Full candidate/employer dashboards are M4.4.

The Bearer token is kept in tab-scoped `sessionStorage`, restored through `/auth/me`, and removed on logout, expiration or an authentication rejection. It survives reloads in that tab; there is no refresh token. A temporary network error offers retry without deleting the session. Logout clears the browser session; a copied token remains valid until expiry unless the account is deactivated. Backend authorization remains authoritative. No passwords or profile summaries are persisted in browser storage.

Auth components, types, session handling and guards are under `src/features/auth/`; the central Axios client attaches tokens and handles expired/inactive sessions.
