# Verified Skill & Career Managed Marketplace

A planned two-tier managed employment marketplace for Bangladesh, with voice-first accessibility. It connects candidates, employers, evaluators and partner training institutes through assessment, verification and managed placement.

## Development status

**Implemented foundation:** M0.1–M0.6 specifications and repository baseline are complete. No application features are implemented. Spring Boot, React and database infrastructure have not been initialized.

**Next:** M1.1 — Finalize Development-Ready Monorepo Structure. Reuse the baseline folders; do not recreate or nest another repository.

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

Compatible runtime/framework versions will be selected during setup.

## Repository structure

| Path | Current purpose |
| --- | --- |
| backend/ | Tracked placeholder; Spring Boot initialization in M1.2 |
| frontend/ | Tracked placeholder; React/TypeScript/Vite and Tailwind setup in M1.3 |
| docs/ | Approved detailed specifications |
| scripts/ | Tracked placeholder; utilities added only when needed |
| MASTER_SPEC.md | Top-level project source of truth |
| .gitignore | Local secrets, editor state and generated/runtime exclusions |
| .editorconfig | UTF-8/LF, final newline, spaces; Java 4-space and default 2-space indentation |
| .env.example | Blank conceptual configuration placeholders only |

Empty skeleton directories use .gitkeep. They contain no source code.

## Documentation

| Document | Purpose |
| --- | --- |
| [MASTER_SPEC.md](MASTER_SPEC.md) | Approved project scope and architecture |
| [ROADMAP.md](docs/ROADMAP.md) | Canonical module sequence, progress and dependency gates |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | System layers, module boundaries and integrations |
| [DATABASE.md](docs/DATABASE.md) | Planned logical schema and relationships |
| [API_SPEC.md](docs/API_SPEC.md) | Planned REST contracts, not implemented endpoints |
| [ROLES_AND_PERMISSIONS.md](docs/ROLES_AND_PERMISSIONS.md) | Roles, ownership and data-access boundaries |
| [BUSINESS_WORKFLOWS.md](docs/BUSINESS_WORKFLOWS.md) | Workflows, states, failure paths and invariants |
| [DEVELOPMENT_STANDARDS.md](docs/DEVELOPMENT_STANDARDS.md) | Engineering, configuration, testing and Git conventions |

## Local development and M0 → M1 handoff

There are no runnable frontend/backend commands or demo accounts yet. Do not run framework generators as part of M0.6.

1. M1.1 — Finalize development-ready monorepo boundaries, framework-owned files and tooling layout.
2. M1.2 — Initialize Java/Spring Boot/Maven.
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
