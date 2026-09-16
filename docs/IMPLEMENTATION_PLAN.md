# Implementation Plan — University Showcase Track

## How to Use

This is the canonical implementation roadmap.

The user may simply ask:

```text
Implement M1.3
```

or:

```text
Implement M2.1 to M2.5
```

Codex must follow `AGENTS.md`, inspect existing code, implement the module/range, run required checks and update `PROGRESS.md`.

Do not renumber modules during implementation.

## Current Baseline

Complete:
- M0.1–M0.6
- M1.1
- M1.2

Next:
- M1.3

---

# M0 — Foundation (COMPLETE)

## M0.1 — Master Product Specification
Goal: define product, users, tracks, business model and MVP.

## M0.2 — Roles, Permissions & Workflows
Goal: define authorization, ownership and main business-state behavior.

## M0.3 — Development Standards
Goal: define engineering/API/security/testing conventions.

## M0.4 — Architecture, Data & API Design
Goal: define modular monolith, data model and REST surface.

## M0.5 — Roadmap & Project Documentation
Goal: create implementation roadmap and references.

## M0.6 — Repository Baseline
Goal: establish backend/frontend/docs/scripts boundaries and Git-safe configuration.

No M0 work should be reimplemented unless this revised showcase spec explicitly supersedes a planning choice.

---

# M1 — Development Environment

## M1.1 — Monorepo Boundaries (COMPLETE)

## M1.2 — Spring Boot Backend Baseline (COMPLETE)

Existing backend behavior must be inspected and preserved.

## M1.3 — React Frontend Initialization

### Goal
Create the polished frontend foundation.

### Requirements
- initialize React + TypeScript + Vite directly under `frontend/`;
- install/configure Tailwind CSS;
- install React Router and Axios;
- use current stable mutually compatible versions;
- add shared app/router/service/type structure;
- add minimal polished development home page;
- add `/` and not-found routing;
- create central API client;
- create typed health API service;
- follow `UI_UX_SHOWCASE_SPEC.md`;
- optional `lucide-react` is allowed for coherent icons.

### Acceptance
- frontend starts;
- Tailwind visibly works;
- router works;
- Axios health client exists;
- `npm run lint` passes;
- `npm run build` passes.

### Out of Scope
Authentication/product dashboards.

## M1.4 — Environment, CORS & Profiles

### Requirements
- frontend `.env.example` with `VITE_API_BASE_URL`;
- backend dev/prod configuration;
- H2 dev/test allowed;
- MySQL environment configuration retained as target;
- configurable CORS origin;
- no real secrets;
- real env files ignored.

### Acceptance
- backend starts without external MySQL in dev;
- frontend config is environment-driven;
- CORS is not wildcarded carelessly;
- backend tests/package and frontend lint/build pass.

## M1.5 — Frontend/Backend Connectivity

### Requirements
- run backend/frontend;
- frontend calls `/api/health`;
- show polished Connected/Unavailable state;
- direct health 200;
- CORS works;
- update setup README.

### Acceptance
- local frontend and backend communicate;
- final M1 builds pass.

---

# M2 — Simplified Database Foundation

M2.1–M2.5 are intentionally designed to be safe as one grouped Codex task.

## M2.1 — Identity & Profiles

### Requirements
Implement:
- User;
- primary role enum;
- account status enum;
- CandidateProfile;
- EmployerProfile;
- repositories/relationships;
- timestamps;
- unique email/profile constraints.

Do not implement authentication endpoints yet.

### Tests
- persistence/context;
- uniqueness/relationship tests.

## M2.2 — Skills, Jobs & Shortlist

### Requirements
Implement:
- Skill;
- CandidateSkill;
- Job;
- ShortlistEntry;
- enums/statuses;
- repository queries needed by later modules.

### Tests
- unique skill link;
- employer job ownership representable;
- duplicate shortlist prevented.

## M2.3 — Assessment, Evaluation & Booking Data

### Requirements
Implement:
- Assessment;
- AssessmentQuestion;
- AssessmentAttempt;
- Evaluation;
- AppointmentSlot;
- Booking.

Use a simple persisted MCQ answer representation; do not create a production-grade assessment engine schema.

### Tests
- relationships/status persistence;
- basic constraints.

## M2.4 — Verification & Training Data

### Requirements
Implement:
- VerificationRecord;
- TrainingProgram;
- Referral.

No NID integration.
No audio/voice DB table required.

### Tests
- verification/referral lifecycle persistence.

## M2.5 — Queue, Placement, Replacement, Notification & Audit Data

### Requirements
Implement:
- WaitingListEntry;
- Placement;
- ReplacementRequest;
- Notification;
- AuditLog.

Add constraints/query support for:
- FIFO queue;
- duplicate active queue membership;
- placement ownership;
- replacement lookup;
- unread notifications.

### Acceptance for M2 Range
- application context starts;
- schema/entities are coherent;
- no business controllers added;
- tests pass;
- no speculative tables beyond the revised model.

---

# M3 — Authentication & Shared Backend

## M3.1 — Common API Infrastructure

Implement:
- global error response using `error`;
- global exception handling;
- minimal DTO/mapping conventions;
- safe audit service helper.

Do not create an abstraction framework.

## M3.2 — Registration, Login & Current User

Implement:
- candidate registration with TECH/TRADE;
- employer registration;
- password hashing;
- login;
- JWT;
- `/api/auth/me`.

No refresh token.

## M3.3 — Security, Roles & Ownership

Implement:
- final Spring Security JWT config;
- ACTIVE account requirement;
- role gates;
- reusable ownership checks where useful;
- authorization tests.

## M3.4 — Frontend Authentication

Implement polished:
- login;
- registration;
- candidate/employer type selection;
- auth state;
- logout;
- protected/role-aware routes;
- API token integration.

Acceptance:
- end-to-end registration/login works;
- role-safe dashboard routing works;
- UI is show-ready.

---

# M4 — Candidate, Employer & Jobs

## M4.1 — Candidate Profile, Skills & CV

Backend + frontend:
- own profile;
- TECH/TRADE fields;
- skills;
- availability;
- secure TECH CV upload;
- polished profile UI.

## M4.2 — Employer Profile & Jobs

Backend + frontend:
- own employer profile;
- job create/edit/close;
- jobs list;
- ownership.

## M4.3 — Candidate Search & Shortlist

- employer-safe candidate projection;
- simple useful filters;
- shortlist candidate for own job;
- duplicate protection;
- polished candidate cards.

Do not implement AI ranking.

## M4.4 — Candidate & Employer Dashboards

Create polished dashboard views based on implemented data.

Acceptance M4:
- TECH candidate core profile workflow works;
- employer can create job/search/shortlist;
- role ownership tests pass;
- dashboards look presentation-ready.

---

# M5 — Assessment, Evaluator & Booking

## M5.1 — Assessment Management & Candidate Attempt

Implement:
- seeded/admin/evaluator assessment management sufficient for demo;
- candidate eligible assessment list;
- start/save/submit MCQ attempt;
- polished assessment UI.

## M5.2 — Scoring + Strategy + Factory

Implement:
- deterministic MCQ scoring;
- `AssessmentStrategy`;
- `TechAssessmentStrategy`;
- `VoiceAssessmentStrategy` or equivalent trade/voice assessment strategy;
- `AssessmentStrategyFactory`.

The pattern must be real and demonstrable.

## M5.3 — Evaluator Review

Implement:
- evaluator work queue;
- attempt detail;
- score/feedback;
- recommendation;
- release result;
- polished evaluator UI.

## M5.4 — Appointments & Booking

Implement:
- slots;
- booking;
- cancellation;
- conflict/capacity handling;
- candidate/evaluator UI.

## M5.5 — Assessment/Evaluator UX Polish

Unify:
- progress;
- status cards;
- result summary;
- evaluator dashboard;
- booking display.

Acceptance:
- full TECH assessment → evaluator workflow demo works;
- Strategy/Factory tests pass.

---

# M6 — TRADE Voice & Verification

## M6.1 — TRADE Onboarding

Create mobile-first trade onboarding:
- category;
- profile fields;
- stepper/progress;
- Bangla-friendly UX;
- manual completion.

## M6.2 — Bangla Voice Input

Implement Web Speech API:
- feature detection;
- Bangla language;
- listening state;
- transcript preview/edit;
- manual fallback.

Do not persist audio.

## M6.3 — Verification Submission & Review

Candidate:
- submit verification information;
- view status.

Evaluator:
- pending list;
- review;
- VERIFIED/FAILED/FLAGGED;
- notes.

## M6.4 — Verified Readiness & Queue Eligibility

Combine verification + evaluation + availability rules into a clear eligibility service used by queue logic.

Do not implement the queue itself here beyond eligibility hooks.

## M6.5 — TRADE UX Polish

Polish:
- mobile layout;
- Bangla copy;
- verification timeline;
- readiness card;
- voice fallback/error states.

Acceptance:
- manual and voice flows both work;
- candidate cannot self-verify;
- TRADE onboarding is visually impressive.

---

# M7 — Placement & Replacement Engine (Hero Feature)

## M7.1 — Waiting List & FIFO

Implement:
- eligible TRADE queue admission;
- queue status;
- FIFO ordering;
- reserve/release;
- duplicate prevention;
- candidate queue view.

## M7.2 — Placement Lifecycle

Implement:
- placement creation from hiring flow;
- ACTIVE/COMPLETED/TERMINATED/REPLACED behavior;
- guarantee flag/expiry;
- candidate/employer placement views.

## M7.3 — ReplacementQueueManager + 24h SLA

Implement the hero feature:
- employer replacement request;
- ownership/coverage checks;
- duplicate active request rejection;
- `ReplacementQueueManager` Spring singleton service;
- FIFO selection;
- atomic reservation;
- empty queue failure;
- accept;
- replacement placement activation;
- SLA result.

High-value tests are mandatory.

## M7.4 — Observer Notifications & Audit

Implement:
- Spring application events;
- notification listener;
- audit listener;
- in-app notification API;
- notification center UI.

No external messaging.

## M7.5 — Replacement Showcase UX

Create excellent UI:
- replacement request modal;
- countdown/progress;
- timeline;
- selected candidate card;
- SLA status;
- employer/candidate/admin views where useful;
- queue visualization.

Acceptance:
- complete replacement demo works end-to-end;
- Singleton and Observer are clearly demonstrable;
- UI is project-show quality.

---

# M8 — Training, Admin & Final Product UX

## M8.1 — Training Referral

Implement:
- training program catalog;
- evaluator/admin referral;
- candidate referral view;
- notification.

Keep simple.

## M8.2 — Essential Admin Console

Implement only high-value admin features:
- user/account status;
- verification/replacement overview;
- basic lists;
- `GET /api/admin/stats`.

No enterprise admin suite.

## M8.3 — Final Landing Page & Shared Design System

Create presentation-grade:
- landing page;
- shared navigation/layout;
- cards/buttons/badges/forms;
- consistent visual system.

## M8.4 — Responsive, Accessibility & State Polish

Audit:
- desktop/mobile;
- TRADE mobile flow;
- loading/error/empty;
- form accessibility;
- focus/contrast;
- status feedback.

## M8.5 — Rich Fictional Demo Data

Create deterministic fictional:
- demo accounts;
- companies;
- jobs;
- candidates;
- skills;
- assessments;
- verification;
- queue;
- placement/replacement;
- notifications;
- training referrals.

No real PII.

Acceptance:
- app feels populated and coherent immediately for presentation.

---

# M9 — Quality, Presentation & Optional Deployment

## M9.1 — High-Value Backend Regression & Security Tests

Add/fix only important tests described in `TESTING_AND_DEMO.md`.

Do not chase 100% coverage.

## M9.2 — Full Integration & Manual E2E

Run/rehearse:
- TECH journey;
- TRADE journey;
- replacement journey;
- role/ownership failures.

Fix visible/functional bugs.

## M9.3 — Academic Pattern Demonstration

Document and verify:
- Strategy;
- Factory;
- Spring Singleton;
- Observer.

Add a short architecture/demo guide for the presentation.

## M9.4 — Final README & Project Show Preparation

Create:
- setup commands;
- demo accounts;
- screenshots placeholders/instructions if useful;
- project explanation;
- demo script;
- final cleanup.

## M9.5 — Simple Deployment (STRETCH)

Only if project is already show-ready and credits/time remain.

Use a simple hosting approach compatible with Spring/React/MySQL/H2.

Do not risk the final demo for this task.

---

# Recommended Codex Batches

For limited usage, use these commands:

1. `Implement M1.3 to M1.5`
2. `Implement M2.1 to M2.5`
3. `Implement M3.1 to M3.4`
4. `Implement M4.1 to M4.4`
5. `Implement M5.1 to M5.5`
6. `Implement M6.1 to M6.5`
7. `Implement M7.1 to M7.5`
8. `Implement M8.1 to M8.5`
9. `Implement M9.1 to M9.4`

M7 is the most important batch. If usage becomes tight, prioritize M7 and M8 visual polish over M9.5 deployment.
