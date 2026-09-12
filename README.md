# Verified Skill & Career Managed Marketplace

A planned two-tier managed employment marketplace for Bangladesh, with voice-first accessibility. It connects candidates, employers, evaluators and partner training institutes through assessment, verification and managed placement.

## Development status

**Current phase:** M1 — Repository & Development Environment. **Completed through:** M1.2 — Spring Boot Backend Initialization. Tests, packaging and live health verification passed. React remains uninitialized; no business features exist.

**Next:** M1.3 — React Frontend Initialization.

## Core tracks and roles

- **Corporate & Tech:** professional profiles, CVs, assessments, interviews and placement.
- **Trade & Field:** simplified Bangla voice-assisted onboarding with permanent manual fallback, trade evaluation and verification.

Canonical roles: CANDIDATE, EMPLOYER, EVALUATOR and ADMIN. TECH and TRADE are candidate types under the shared CANDIDATE role, not separate authentication roles.

## Planned features

Candidate profiles and skills; employer/job workflows; assessments and evaluator reviews; appointments; Bangla voice-assisted trade onboarding; verification; waiting lists; managed placements; training referrals; in-app notifications; audit logging.

### Planned 24-hour replacement workflow

For eligible managed Trade placements, employers may request replacement within the recorded guarantee period. The future engine will select verified, eligible workers from a FIFO waiting list, reserve a candidate, obtain both parties' confirmation and track a 24-hour SLA through the replacement's actual start. Coverage duration and the SLA are separate. This workflow is not implemented yet.

## Planned technology stack

| Area | Stack |
| --- | --- |
| Frontend | React, TypeScript, Vite, Tailwind CSS, React Router, Axios |
| Backend | Java, Spring Boot, Maven, Spring Security, Spring Data JPA, Bean Validation |
| Database | MySQL; H2 for suitable development/test scenarios |
| Voice | Browser Web Speech API for Bangla input where supported; manual fallback |
| Development | Git/GitHub, Postman, VS Code or IntelliJ IDEA |

Backend: Java 21, Spring Boot 4.1.1, Maven 3.9.16 through the included wrapper. Frontend versions remain for M1.3.

## Repository structure

| Path | Current purpose |
| --- | --- |
| backend/ | [Spring Boot backend](backend/README.md); bootstrap, public health check and infrastructure tests |
| frontend/ | [Frontend application boundary](frontend/README.md); React initialization in M1.3 |
| docs/ | Approved detailed specifications |
| scripts/ | [Project helpers policy](scripts/README.md); utilities added only when needed |
| MASTER_SPEC.md | Top-level project source of truth |
| .gitignore | Local secrets, editor state and generated/runtime exclusions |
| .editorconfig | UTF-8/LF, final newline, spaces; Java 4-space and default 2-space indentation |
| .env.example | Blank conceptual configuration placeholders only |

This simple monorepo contains one backend application, one frontend application and shared documentation in one Git repository. Workspace READMEs replace empty placeholders. There are no root package-manager workspaces or Maven multi-module build. Application-specific configuration and source stay within the corresponding application root.

## Documentation

| Document | Purpose |
| --- | --- |
| [MASTER_SPEC.md](MASTER_SPEC.md) | Approved project scope and architecture |
| [ROADMAP.md](docs/ROADMAP.md) | Canonical module sequence, progress and dependency gates |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | System layers, module boundaries and integrations |
| [DATABASE.md](docs/DATABASE.md) | Planned logical schema and relationships |
| [API_SPEC.md](docs/API_SPEC.md) | Implemented health contract and planned business REST contracts |
| [ROLES_AND_PERMISSIONS.md](docs/ROLES_AND_PERMISSIONS.md) | Roles, ownership and data-access boundaries |
| [BUSINESS_WORKFLOWS.md](docs/BUSINESS_WORKFLOWS.md) | Workflows, states, failure paths and invariants |
| [DEVELOPMENT_STANDARDS.md](docs/DEVELOPMENT_STANDARDS.md) | Engineering, configuration, testing and Git conventions |

## Local development and M0 → M1 handoff

With JDK 21 selected, run backend commands from `backend/`:

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

Check `http://localhost:8080/api/health` for `{"status":"UP"}`. See [backend instructions](backend/README.md) for packaging, Windows commands, dependencies and the temporary security/database baseline. No frontend commands or demo accounts exist yet. Planned frontend origin is `http://localhost:5173`; actual environment/CORS configuration belongs to M1.4.

1. M1.1 — Complete: development-ready monorepo boundaries, framework-owned files and tooling layout.
2. M1.2 — Complete: Java/Spring Boot/Maven infrastructure, tests, build and health verification.
3. M1.3 — Initialize React/TypeScript/Vite and Tailwind.
4. M1.4 — Establish actual environment variables, loading, profiles and CORS.
5. M1.5 — Verify builds and frontend/backend/database connectivity.

The root .env.example documents possible variables only. It is not automatically loaded and its blank values do not configure a working application. Setup instructions and run/test commands will be added in M1.

## Environment and security notes

- DB_URL, DB_USERNAME and DB_PASSWORD describe backend database configuration; JWT_SECRET is backend-only.
- FRONTEND_URL and CORS_ALLOWED_ORIGINS describe deployment origins. VITE_API_BASE_URL is a public API address.
- Actual names/defaults/loading are deferred to M1.4. Never put database or signing secrets into VITE_* values.
- .env and environment-specific .env.* files are ignored; example files are explicitly retained.
- No real credentials, identity documents or personal demo data should be committed.
- Root uploads/ and backend/uploads/ are reserved ignored local runtime locations, not an implemented storage system.
- Maven wrappers and application source must remain trackable when added later.
- No license has been selected or LICENSE file added.

See the development standards before making changes.
