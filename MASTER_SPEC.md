# Verified Skill & Career Managed Marketplace with Voice-First Accessibility

> University Showcase Master Specification  
> Revised for high showcase value, strong UI, and feasible Codex implementation.  
> Current implementation baseline: complete through M1.2.

## 1. Product Goal

Build a polished two-track employment marketplace for Bangladesh that demonstrates:

- verified/assessed candidate readiness;
- employer-side hiring workflows;
- accessible Bangla voice-assisted onboarding for trade workers;
- managed placement;
- a skill/category waiting list;
- an automated replacement workflow with a tracked 24-hour SLA;
- meaningful OOP/design-pattern usage.

This is a **university project and project-show submission**, not a production SaaS platform. It must look and behave convincingly, but it should avoid infrastructure that does not materially improve the demonstration.

## 2. Core Tracks

### TECH

Target users:
- CSE students;
- developers;
- engineers;
- graduates;
- corporate professionals.

Core journey:

```text
Register
→ Profile + Skills + CV
→ Assessment
→ Evaluator Review
→ Employer Search/Shortlist
→ Interview/Booking
→ Placement
```

### TRADE

Target users:
- drivers;
- electricians;
- AC technicians;
- mechanics;
- delivery riders;
- plumbers;
- similar skilled/semi-skilled workers.

Core journey:

```text
Register
→ Trade Category
→ Bangla Manual/Voice-Assisted Onboarding
→ Verification
→ Skill Evaluation
→ Waiting List
→ Managed Placement
→ Replacement Pool
```

## 3. Roles

Authentication roles:

- `CANDIDATE`
- `EMPLOYER`
- `EVALUATOR`
- `ADMIN`

Candidate types:

- `TECH`
- `TRADE`

Candidate type is not an authentication role.

For showcase simplicity, the revised MVP assumes one primary operational role per user account. Do not implement complex multi-role organization membership.

## 4. Core Differentiator — Managed Replacement

Eligible managed TRADE placements can receive a replacement request during an active guarantee window.

Core workflow:

1. Employer requests replacement for its own eligible placement.
2. Backend validates ownership and eligibility.
3. Replacement request receives a 24-hour target timestamp.
4. Queue manager derives the required trade/skill.
5. It selects the first currently eligible waiting-list candidate using FIFO.
6. Candidate is reserved atomically.
7. Employer/candidate receive in-app notification.
8. Replacement is accepted/confirmed in the demo workflow.
9. A replacement placement becomes active.
10. Request records `ON_TIME` or `BREACHED`.

The project tracks an operational SLA. It does not claim that software can guarantee human availability.

### Queue Policy

Among candidates who satisfy all eligibility rules:

```text
FIFO by waiting-list joined time
→ stable ID tie-breaker
```

Assessment scores do not reorder otherwise eligible candidates.

### Eligibility

A replacement candidate must be:

- `TRADE`;
- account `ACTIVE`;
- verification `VERIFIED`;
- appropriate trade/skill;
- `AVAILABLE`;
- not already reserved;
- not in an incompatible active placement.

## 5. Business Model

Primary B2B concepts:

- corporate placement fee;
- verified workforce/service fee;
- training referral commission.

Candidates are not charged for basic access to job opportunities.

No payment gateway, invoicing engine or financial accounting is required for the university MVP.

## 6. Required Showcase Features

### Account & Authentication
- candidate registration;
- employer registration;
- evaluator/admin demo accounts;
- login/logout;
- JWT authentication;
- role authorization.

### Candidate
- TECH/TRADE profile;
- skills;
- availability;
- TECH CV upload;
- candidate dashboard.

### Employer
- company profile;
- job creation/management;
- eligible candidate search;
- shortlist;
- placement/replacement dashboard.

### Assessment
- simple MCQ assessment;
- assessment attempt;
- automatic score for supported questions;
- evaluator review;
- `HIRE_READY`, `NEEDS_TRAINING`, `REJECTED`.

### Evaluator
- pending evaluation list;
- candidate submission review;
- score/feedback/recommendation;
- interview/consultation notes where used.

### Booking
- simple appointment/interview slots;
- booking;
- conflict prevention;
- cancellation.

### TRADE Voice Experience
- Bangla-first onboarding;
- Web Speech API speech-to-text where supported;
- transcript preview/edit;
- permanent manual fallback.

Voice input is accessibility assistance, not biometric authentication.

### Verification
- manual/platform verification record;
- `PENDING`, `IN_REVIEW`, `VERIFIED`, `FAILED`, `FLAGGED`;
- restricted reviewer notes;
- employer sees safe verification status only.

Do not claim government NID validation.

### Waiting List
- eligible verified TRADE candidate enters queue;
- category/skill-based queue;
- FIFO ordering;
- queue position/status visible in a polished way.

### Placement
- create/manage candidate-employer placement;
- active/completed/terminated states;
- guarantee eligibility metadata.

### Replacement
- request;
- candidate selection;
- reservation;
- status timeline;
- 24-hour SLA tracking;
- in-app notifications;
- failure when no candidate is available.

### Training Referral
- simple training-program catalog;
- evaluator/admin referral after `NEEDS_TRAINING`;
- candidate can view referral.

### Notifications
- in-app only;
- unread/read state.

### Admin
Essential showcase console:
- users;
- jobs/reference data overview;
- verification cases;
- placements/replacements;
- simple high-value statistics.

No enterprise admin suite is required.

### Audit
Basic immutable audit entries for important operations:
- verification decision;
- evaluation completion;
- placement/replacement;
- account/status/admin actions.

A sophisticated audit-search product is not required.

## 7. Showcase UI Quality

Visible quality is a first-class requirement.

The final project must include:

- high-quality landing page;
- polished authentication screens;
- role-specific dashboards;
- attractive data cards/status chips;
- meaningful empty/loading/error states;
- responsive layouts;
- mobile-first TRADE flow;
- Bangla voice controls;
- queue-position visualization;
- replacement timeline/countdown;
- assessment progress UI;
- accessible forms;
- consistent spacing/typography/colors;
- realistic fictional demo data.

See `docs/UI_UX_SHOWCASE_SPEC.md`.

## 8. OOP / Design Patterns

### Strategy

Assessment behavior varies by candidate/evaluation context.

Expected:

```text
AssessmentStrategy
├── TechAssessmentStrategy
└── VoiceAssessmentStrategy
```

Exact method signatures are implementation details.

### Factory

A small factory resolves the appropriate assessment strategy.

### Singleton

A central `ReplacementQueueManager` is a Spring-managed service. Spring services are singleton scoped by default.

Do not implement unsafe global static mutable state.

### Observer

Use Spring application events/listeners for:
- candidate verified;
- evaluation completed;
- placement created;
- replacement requested/selected/completed;
- referral created.

Listeners may create:
- in-app notifications;
- audit entries.

No Kafka/RabbitMQ is required.

## 9. Technology Stack

### Backend
- Java 21 baseline already established;
- Spring Boot baseline already established in M1.2;
- Maven;
- Spring MVC;
- Spring Data JPA;
- Spring Security;
- Bean Validation;
- JWT.

### Database
- H2 is acceptable for development/tests/demo bootstrap;
- MySQL remains compatible target and can be used for final demonstration/deployment if convenient.

Do not spend large implementation effort on database operations infrastructure.

### Frontend
- React;
- TypeScript;
- Vite;
- Tailwind CSS;
- React Router;
- Axios.

Use mutually compatible stable versions selected at M1.3 and record them.

### Voice
- browser Web Speech API;
- Bangla recognition when browser supports it;
- manual fallback.

## 10. Current Backend Baseline

M1.2 is complete.

The established backend includes:
- Java 21 project baseline;
- Spring Boot backend under `backend/`;
- Maven Wrapper;
- public `GET /api/health`;
- temporary security baseline;
- H2 bootstrap;
- backend test/package/startup verification.

No business domain schema, JWT auth or frontend is assumed complete before M1.3.

Codex must inspect actual repository code before changing this baseline.

## 11. Simplified Data Strategy

The university MVP intentionally avoids unnecessary table proliferation.

Core logical entities are described in `docs/DATA_MODEL.md`.

Important simplifications:
- one primary `role` per User rather than a complex multi-role subsystem;
- voice transcript does not require permanent storage for the showcase;
- assessment answers may use a simple persisted representation suitable for MCQ;
- CV metadata can remain part of candidate profile/storage metadata;
- training provider data is simplified;
- analytics are derived from existing tables rather than a data warehouse.

## 12. Security Principles

- backend enforces authorization;
- passwords are hashed;
- JWT secret is backend-only;
- user cannot edit authoritative statuses;
- employer cannot see raw verification evidence;
- candidate cannot self-verify or self-score;
- queue priority is server-controlled;
- replacement requires placement ownership;
- real secrets are never committed.

See `docs/SECURITY.md`.

## 13. API Principles

- prefix `/api`;
- typed request/response DTOs;
- no direct entity exposure;
- meaningful HTTP status codes;
- consistent safe errors;
- explicit actions for important state transitions;
- avoid unnecessary API versioning for the university MVP.

See `docs/API_SPEC.md`.

## 14. What Is Intentionally Removed / Deferred

Do not implement unless explicitly requested:

- real government NID API;
- SMS/voice-call gateway;
- production email infrastructure;
- payment gateway;
- billing/invoicing;
- multi-organization employer membership;
- refresh-token subsystem;
- advanced analytics/BI;
- AI CV ranking;
- AI interviews;
- ML queue ranking;
- WebSockets/realtime push;
- microservices;
- Kafka/RabbitMQ;
- Redis;
- Kubernetes;
- complex Docker orchestration;
- distributed transactions;
- advanced audit platform;
- exhaustive enterprise test coverage;
- native mobile apps.

## 15. Demo Narrative

The final showcase should make this flow visually impressive:

```text
TRADE candidate registers
→ selects "AC Technician"
→ uses Bangla voice input
→ verification becomes VERIFIED
→ candidate enters AC Technician queue
→ employer has active managed placement
→ worker becomes unavailable
→ employer requests replacement
→ queue manager selects next eligible worker
→ replacement timeline starts
→ notification appears
→ replacement placement becomes ACTIVE
→ SLA becomes ON_TIME
→ admin dashboard reflects the event
```

During presentation, explicitly identify:
- Strategy;
- Factory;
- Singleton;
- Observer.

## 16. Success Criteria

The project is successful when:

1. core workflows work end-to-end;
2. the replacement engine is demonstrable and understandable;
3. UI looks polished enough for a project show;
4. mobile TRADE UX is compelling;
5. roles and security boundaries are credible;
6. OOP patterns are visible in real code;
7. demo data makes the system feel alive;
8. no major screen looks unfinished;
9. builds/tests are reliable;
10. the project can be explained clearly within a short presentation.
