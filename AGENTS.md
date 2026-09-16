# AGENTS.md — Codex Operating Rules

## Project

This repository contains **Verified Skill & Career Managed Marketplace with Voice-First Accessibility**, a university showcase project for Bangladesh.

The application has two candidate tracks:

- **TECH** — students, engineers, developers and other professional candidates.
- **TRADE** — drivers, electricians, technicians, mechanics, riders and other field workers, with Bangla-first and voice-assisted onboarding.

Authentication roles are:

- `CANDIDATE`
- `EMPLOYER`
- `EVALUATOR`
- `ADMIN`

`TECH` and `TRADE` are candidate types, not roles.

The showcase must feel polished and complete, but it is **not a production-scale enterprise system**. Prefer a strong, demonstrable MVP over infrastructure that will not improve the university presentation.

## Canonical Documentation

Before implementation, use this hierarchy:

1. `AGENTS.md` — how Codex must work.
2. `docs/PROGRESS.md` — what is already done.
3. `docs/IMPLEMENTATION_PLAN.md` — exact module/range requirements.
4. `MASTER_SPEC.md` — product scope and non-negotiable requirements.
5. Relevant supporting documents only:
   - `docs/ARCHITECTURE.md`
   - `docs/DATA_MODEL.md`
   - `docs/API_SPEC.md`
   - `docs/BUSINESS_WORKFLOWS.md`
   - `docs/SECURITY.md`
   - `docs/UI_UX_SHOWCASE_SPEC.md`
   - `docs/DEVELOPMENT_STANDARDS.md`
   - `docs/TESTING_AND_DEMO.md`
   - `docs/DECISIONS.md`

Do not read every large document for a small task. Read the parts relevant to the requested module.

## Short User Commands

The user should not need to provide giant prompts.

Treat these as valid implementation requests:

- `Implement M1.3`
- `Implement 1.3`
- `Implement M2.1 to M2.5`
- `Implement M4`
- `Continue M7.3`
- `Resume the previous task`

If the `M` prefix is omitted, infer it from `IMPLEMENTATION_PLAN.md`.

For an inclusive range such as `M2.1 to M2.5`:

1. implement modules in numeric order;
2. inspect once up front;
3. satisfy each module's acceptance criteria;
4. use targeted checks while working;
5. run the broader relevant build/test suite at the end of the batch;
6. update each completed module in `PROGRESS.md`.

Do not require the user to restate requirements that already exist in the repository.

## Existing-Code-First Rule

Before editing:

1. inspect repository structure;
2. run `git status`;
3. inspect `git diff`;
4. inspect relevant existing code/config/tests;
5. determine whether part of the requested module already exists.

Extend working code instead of recreating it.

Do not:
- redo completed work;
- rewrite working architecture merely for preference;
- rename APIs without a requirement;
- refactor unrelated modules;
- add dependencies without a concrete need;
- create placeholder files for future modules.

## Interrupted Task Recovery

If a Codex run stopped because of usage limits or interruption:

1. inspect `git status` and `git diff`;
2. inspect modified/untracked files;
3. compare current code to the requested module acceptance criteria;
4. identify completed and unfinished requirements;
5. continue only unfinished work;
6. run the required checks;
7. update `PROGRESS.md` only after actual completion.

Never assume an interrupted run made zero progress.

## Usage-Efficiency Rules

This project is being completed with a limited Codex allowance.

Therefore:

- do not perform unrelated cleanup;
- do not introduce enterprise infrastructure;
- do not repeatedly run the entire test suite after every tiny edit;
- use targeted tests during implementation and a complete relevant check at the module/batch boundary;
- do not generate extensive new documentation unless the implementation changes a decision;
- do not create abstractions for one-off behavior;
- do not over-engineer pagination, caching, background jobs or deployment;
- do not add Docker/Kubernetes/message brokers/microservices unless explicitly requested;
- do not build features marked `STRETCH`;
- keep completion summaries concise.

The goal is **maximum showcase value per unit of implementation work**.

## Quality Must Not Be Reduced

Scope is reduced, but visible quality is not.

Every user-facing module must follow `docs/UI_UX_SHOWCASE_SPEC.md`.

Required:
- responsive design;
- consistent design tokens/components;
- proper loading/error/empty states;
- useful validation;
- mobile-friendly TRADE workflow;
- polished dashboards;
- attractive replacement/queue visualization;
- clean Bangla voice interaction;
- no obvious unfinished placeholder UI.

## Backend Rules

- Java/Spring Boot/Maven backend under `backend/`.
- Base package: `com.marketplace`.
- Controllers are thin.
- Business rules live in services.
- Repositories handle persistence.
- DTOs define API contracts.
- JPA entities are not returned directly from controllers.
- Backend is the final security authority.
- Use Bean Validation for request shape.
- Protect ownership and state transitions in services.
- Use transactions around atomic multi-record operations.

## Frontend Rules

- React + TypeScript + Vite + Tailwind CSS + React Router + Axios.
- Central configured Axios client.
- Feature-oriented UI/services.
- Avoid widespread `any`.
- Do not put secrets in `VITE_*`.
- Route guards are UX, not backend security.
- Use manual fallback for all voice-assisted fields.

## Security Rules

Never:
- commit passwords/tokens/JWT secrets/DB credentials;
- expose password hashes;
- expose raw verification/NID evidence to employers;
- let clients control role, verification status, assessment score, queue priority, placement status, replacement status or SLA status;
- trust a client-supplied user/candidate/employer ID as authorization.

The MVP uses platform/manual verification. Do not claim real government NID integration.

## OOP / Academic Requirements

The final project must clearly demonstrate:

- **Strategy** — assessment behavior (`TechAssessmentStrategy`, `VoiceAssessmentStrategy` or equivalent).
- **Factory** — strategy selection/creation.
- **Singleton** — central replacement queue manager using Spring's normal singleton service scope, not unsafe static state.
- **Observer** — Spring application events/listeners for notifications/audit reactions.

Do not add artificial patterns elsewhere just to increase pattern count.

## Progress Tracking

`docs/PROGRESS.md` is the canonical implementation tracker.

After completing a module:
- change its status to `✅ COMPLETE`;
- set the next incomplete module;
- do not mark a module complete if tests/build/acceptance criteria fail;
- do not mark a later module complete because a small supporting piece exists.

Current starting state:
- everything through M1.2 is complete;
- M1.3 is the next canonical task.

## Definition of Done

A module is complete when applicable:

- functional requirements work;
- security/ownership rules work;
- user-facing states are polished;
- meaningful tests/checks pass;
- backend/frontend builds pass for the affected area;
- no secrets are introduced;
- no unrelated features were implemented;
- `PROGRESS.md` is updated.

## Completion Response

Report only:

- module(s) completed;
- major changes;
- important files changed;
- tests/checks and result;
- documentation/progress updated;
- anything genuinely unfinished/blocking.

Do not produce a massive report unless the user asks.
