# Frontend Workspace

`frontend/` is the future React application root. M1.3 owns React/TypeScript/Vite, Tailwind CSS, React Router and Axios initialization and build verification. No frontend commands or components exist yet.

Planned package name: `verified-career-marketplace-frontend`. Package management, lockfile, `package.json`, `vite.config.ts`, `tsconfig.*`, `index.html` and `src/` belong here. No root npm workspace is required. Run future frontend commands from this directory; generated output belongs in ignored `frontend/dist/`.

Expected local origin: `http://localhost:5173`; backend API: `http://localhost:8080/api`. M1.4 owns actual environment configuration and explicit backend CORS permission for the frontend origin. Only public `VITE_API_BASE_URL` belongs in browser configuration. All `VITE_*` values are public; never include JWT signing secrets or database credentials. Root `.env.example` remains a conceptual reference until application-specific examples are needed.

Follow [architecture](../docs/ARCHITECTURE.md) and [standards](../docs/DEVELOPMENT_STANDARDS.md) for feature-oriented organization, including plural `candidates` and `employers`. Role navigation supports UX; the backend is the authorization authority. Trade/Field UX must prioritize Bangla, minimal typing and permanent manual fallback.
