# Verified Skill & Career Managed Marketplace

University showcase edition.

A two-track managed employment platform for Bangladesh with:
- TECH candidate assessment/hiring;
- TRADE voice-assisted onboarding;
- verification;
- FIFO workforce waiting list;
- managed placement;
- 24-hour replacement SLA workflow.

## Current Status

Completed through:
- M3.4 — Frontend Authentication

Next:
- M4.1 — Candidate Profile, Skills & CV

See:
- [AGENTS.md](AGENTS.md)
- [MASTER_SPEC.md](MASTER_SPEC.md)
- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
- [Progress](docs/PROGRESS.md)

## Quick Development

Prerequisites: **JDK 21**, **Node.js 22.12+** (Node 24 LTS recommended), and npm. Set `JAVA_HOME` to JDK 21 if needed. The included Maven wrapper downloads Maven; no global Maven is required.

Backend (terminal 1):

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

Health:

```text
http://localhost:8080/api/health
```

Frontend (terminal 2):

```bash
cd frontend
npm ci
cp .env.example .env
npm run dev
```

Open **http://localhost:5173**. The home page should show **Connected**. If the service is unavailable, start the backend and select **Check again**.

The backend defaults to the `dev` profile with ephemeral H2; no external database is required. `CORS_ALLOWED_ORIGINS` accepts exact comma-separated browser origins and defaults to `http://localhost:5173` in dev. If using a different browser origin, set it in the backend environment. `VITE_API_BASE_URL` controls the public API address (including `/api`); restart Vite after changes.

For MySQL, select `SPRING_PROFILES_ACTIVE=prod` and supply `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `CORS_ALLOWED_ORIGINS`. Production validates an already provisioned schema; dev/test create the M2 entity schema in ephemeral H2. Spring Boot reads process environment variables and does **not** automatically load `.env`. See the application examples and [backend setup](backend/README.md).

Checks:

```bash
cd backend
./mvnw clean package
cd ../frontend
npm run lint
npm run build
```

See [frontend setup](frontend/README.md) for structure and preview instructions. Registration, login and role-aware account pages are implemented. Full product dashboards are planned in M4.4.

## Codex Workflow

You should be able to use short commands:

```text
Implement M1.3
```

or:

```text
Implement M2.1 to M2.5
```

Codex must read `AGENTS.md` and repository documentation instead of requiring a giant prompt.

## Showcase Priority

1. Core workflows
2. Replacement engine
3. UI/UX quality
4. Role/security credibility
5. OOP pattern demonstration
6. Demo reliability
7. Optional deployment
