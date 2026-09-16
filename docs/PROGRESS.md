# Implementation Progress

Legend:
- ✅ COMPLETE
- 🚧 IN PROGRESS
- ⬜ NOT STARTED
- ⛔ BLOCKED
- ⭐ STRETCH

## M0 — Foundation
- ✅ M0.1 — Master Product Specification
- ✅ M0.2 — Roles, Permissions & Workflows
- ✅ M0.3 — Development Standards
- ✅ M0.4 — Architecture, Data & API Design
- ✅ M0.5 — Roadmap & Project Documentation
- ✅ M0.6 — Repository Baseline

## M1 — Development Environment
- ✅ M1.1 — Monorepo Boundaries
- ✅ M1.2 — Spring Boot Backend Baseline
- ✅ COMPLETE — M1.3 — React Frontend Initialization
- ✅ COMPLETE — M1.4 — Environment, CORS & Profiles
- ✅ COMPLETE — M1.5 — Frontend/Backend Connectivity

## M2 — Simplified Database Foundation
- ✅ COMPLETE — M2.1 — Identity & Profiles
- ✅ COMPLETE — M2.2 — Skills, Jobs & Shortlist
- ✅ COMPLETE — M2.3 — Assessment, Evaluation & Booking Data
- ✅ COMPLETE — M2.4 — Verification & Training Data
- ✅ COMPLETE — M2.5 — Queue, Placement, Replacement, Notification & Audit Data

## M3 — Authentication & Shared Backend
- ✅ COMPLETE — M3.1 — Common API Infrastructure
- ✅ COMPLETE — M3.2 — Registration, Login & Current User
- ✅ COMPLETE — M3.3 — Security, Roles & Ownership
- ✅ COMPLETE — M3.4 — Frontend Authentication

## M4 — Candidate, Employer & Jobs
- ✅ COMPLETE — M4.1 — Candidate Profile, Skills & CV
- ✅ COMPLETE — M4.2 — Employer Profile & Jobs
- ✅ COMPLETE — M4.3 — Candidate Search & Shortlist
- ✅ COMPLETE — M4.4 — Candidate & Employer Dashboards

## M5 — Assessment, Evaluator & Booking
- ✅ COMPLETE — M5.1 — Assessment Management & Candidate Attempt
- ✅ COMPLETE — M5.2 — Scoring + Strategy + Factory
- ✅ COMPLETE — M5.3 — Evaluator Review
- ✅ COMPLETE — M5.4 — Appointments & Booking
- ✅ COMPLETE — M5.5 — Assessment/Evaluator UX Polish

## M6 — TRADE Voice & Verification
- ✅ COMPLETE — M6.1 — TRADE Onboarding
- ✅ COMPLETE — M6.2 — Bangla Voice Input
- ✅ COMPLETE — M6.3 — Verification Submission & Review
- ✅ COMPLETE — M6.4 — Verified Readiness & Queue Eligibility
- ✅ COMPLETE — M6.5 — TRADE UX Polish

## M7 — Placement & Replacement Engine
- ✅ COMPLETE — M7.1 — Waiting List & FIFO
- ✅ COMPLETE — M7.2 — Placement Lifecycle
- ✅ COMPLETE — M7.3 — ReplacementQueueManager + 24h SLA
- ✅ COMPLETE — M7.4 — Observer Notifications & Audit
- ✅ COMPLETE — M7.5 — Replacement Showcase UX

## M8 — Training, Admin & Final Product UX
- ✅ COMPLETE — M8.1 — Training Referral
- ✅ COMPLETE — M8.2 — Essential Admin Console
- ✅ COMPLETE — M8.3 — Final Landing Page & Shared Design System
- ✅ COMPLETE — M8.4 — Responsive, Accessibility & State Polish
- ✅ COMPLETE — M8.5 — Rich Fictional Demo Data

## M9 — Quality, Presentation & Deployment
- ⬜ M9.1 — High-Value Backend Regression & Security Tests
- ⬜ M9.2 — Full Integration & Manual E2E
- ⬜ M9.3 — Academic Pattern Demonstration
- ⬜ M9.4 — Final README & Project Show Preparation
- ⭐ M9.5 — Simple Deployment (Stretch)

## Current Next Task

**M9.1 — High-Value Backend Regression & Security Tests**

Recommended next batch:

```text
Implement M9.1 to M9.4. Follow AGENTS.md and the project documentation.
```

## M1.3–M1.5 Validation — 2026-09-13

- React/TypeScript/Vite/Tailwind frontend, home/404 routes, typed Axios health client and connection/retry states implemented.
- Environment examples, explicit CORS origins, H2 dev/test and MySQL prod profiles implemented; setup READMEs updated.
- `npm run lint` and `npm run build`: PASS.
- `./mvnw clean package`: PASS; 7 tests, no failures/errors/skips.
- Live default-dev startup without MySQL; direct `/api/health` 200; allowed browser origin works and untrusted origin returns 403.
- Chromium checks: desktop and 375px mobile layout, no horizontal overflow, loading, unavailable/retry recovery, not-found/home navigation; no runtime errors.
- Validation used temporary Node 24.21.0 and Temurin JDK 21.0.12.1 outside the repository because the host lacked npm/JDK 21. MySQL production connectivity is not exercised in M1.

## M2.1–M2.5 Validation — 2026-09-13

- Added all 21 documented entities and repositories with generated IDs, string enums, foreign keys, timestamps and relevant uniqueness/check constraints.
- Added ownership, FIFO, booking overlap, released evaluation, active replacement and unread notification queries; row-lock/version support is ready for later transactional services.
- Queue constraints prevent duplicate active membership and cross-skill double reservation while preserving exited history. Replacement requests link the resulting placement and prevent conflicting active requests.
- `./mvnw clean package`: PASS; 34 tests, no failures/errors/skips (27 new persistence tests plus 7 existing tests).
- `npm run lint` and `npm run build`: PASS; frontend unchanged.
- Packaged default-dev application starts with all mappings in H2; live `/api/health` returned HTTP 200 and `{"status":"UP"}` on temporary port 18082.
- Data-model decisions and setup READMEs updated. No new dependencies, business controllers, authentication endpoints, seed data or speculative tables added.
- Validation used existing temporary JDK 21 and Node 24 runtimes outside the repository. Live MySQL remains untested; business authorization, eligibility, booking capacity and replacement orchestration remain in their planned later modules.

## M3.1–M3.4 Validation — 2026-09-13

- Added the shared `error` contract, global validation/exception handling, explicit request/response records and an allowlisted audit helper that stores no credentials or request bodies.
- Atomic TECH/TRADE candidate and employer signup, BCrypt passwords, login, expiring JWT and safe `/api/auth/me` implemented. Public signup cannot assign operational roles or authoritative statuses.
- Stateless security checks current database role and ACTIVE status on each authenticated request; namespace role gates, method-security support and reusable ownership checks implemented.
- Added responsive login/register screens, track/account choices, Bangla TRADE labels, validation, password visibility, loading/errors, tab-scoped session restoration, expiration/logout and role-safe account routes. Full product dashboards remain M4.4.
- `./mvnw clean package`: PASS; 45 tests, no failures/errors/skips, including 11 new authentication/JWT tests. Covers invalid/expired/wrong-signature tokens, privileged signup rejection, role changes, all inactive statuses, duplicate registration, ownership and CORS.
- `npm run lint` and `npm run build`: PASS.
- Live packaged backend + Chromium: TECH, TRADE and employer signup; login failure/success; password mismatch/show toggle; submission loading; reload restoration; cross-role redirects; network retry; logout; invalid-session recovery; desktop and 375px mobile layout. PASS, no runtime errors or horizontal overflow; screenshots visually reviewed.
- API contract, JWT environment example and setup READMEs updated. No dependencies added. Existing uncommitted work preserved.
- Validation used existing temporary JDK 21/Node 24 runtimes and isolated ports 18083/15173. MySQL was not exercised. Dev/test can use a generated per-start signing key; production requires `JWT_SECRET`. No refresh tokens or privileged demo accounts were added.

## M4.1–M4.4 Validation — 2026-09-14

- Implemented candidate self-profile, immutable TECH/TRADE track fields, self-reported skills and availability; starter skill catalog seeds idempotently at application startup (disabled for isolated test fixtures).
- TECH PDF CV upload validates size (5 MB), extension, MIME type, signature and filename; private generated storage names, authenticated attachment downloads, owner checks, replacement/rollback cleanup. No public file URLs or stored paths are exposed.
- Implemented employer self-profile, owned job create/edit/close/list/detail, server-controlled ACTIVE/CLOSED transitions and transactionally serialized shortlist additions with duplicate and track/skill/availability checks.
- Added bounded candidate search with track/location/skill/availability filters and explicit employer-safe cards. Private identity/contact data and internal notes are excluded; only released evaluation results are returned. Search/shortlisting does not imply verified placement eligibility.
- Added responsive candidate/employer dashboards, profile editors, jobs, search and shortlist views using shared design tokens, actionable empty states, validation/success feedback, loading/retry and Bangla TRADE labels. Skill/CV actions preserve unsaved profile text. Later booking, placement, queue, voice and notification workflows remain in their planned modules.
- `./mvnw clean package`: PASS; 49 tests, no failures/errors/skips. Four new integration scenarios cover profile/track authority, CV validation/access/replacement, job/shortlist ownership and transitions, filtered search and private/unreleased data exclusion. Fixed starter-catalog collision with existing persistence fixtures by disabling automatic seeds in the test profile.
- `npm run lint` and `npm run build`: PASS.
- Live packaged backend + Chromium: TECH profile/skills/CV upload/download and reload; employer profile; job create/edit/close; search/filter/shortlist/remove; dashboard data; TRADE profile; loading/network retry; cross-role redirects. Desktop and 375px mobile layouts PASS with no horizontal overflow or runtime errors; screenshots visually reviewed.
- Updated API contracts, backend storage setup and environment example. No dependencies added; existing uncommitted work preserved. Validation used temporary JDK 21/Node 24 and isolated ports 18084/15174. MySQL was not exercised; CV validation is not malware scanning.

## M5.1–M5.5 Validation — 2026-09-14

- Added idempotent seeded TECH/TRADE demo assessments, track eligibility, one resumable attempt, saved MCQ answers and atomic submission. Candidate DTOs omit answer keys and keep scores/results private until release; ownership and immutable submitted answers are enforced by services.
- Implemented real Strategy/Factory selection: TECH weighted percentage scoring (two-decimal rounding); TRADE/Voice defers scoring to manual practical review. Demo attempts are untimed; voice capture remains M6.
- Added evaluator work queue/detail, score/feedback/recommendation drafts, evaluator ownership and explicit final release. Internal notes remain restricted. Optional dev-only evaluator bootstrap uses user-supplied environment credentials and BCrypt; no default password or public privileged signup.
- Added evaluator-owned slot creation/closing, candidate booking/history/cancellation, future-time validation, capacity and overlapping booking protection. Candidate/slot/evaluator row locks serialize conflicting writes. Normalized slot timestamps to milliseconds after the full suite exposed a database rounding issue with adjacent slots.
- Added responsive assessment progress/question navigation/save/submit confirmation, released result summaries, evaluator dashboard/review/release, candidate dashboard activity and appointment screens. Loading/error/retry/empty states and Bangla/English TRADE questions included.
- `./mvnw clean package`: PASS; 55 tests, no failures/errors/skips. Six new tests cover Strategy/Factory, complete TECH review/release, TRADE manual scoring, ownership/roles, validation, private results, slot conflicts/cancellation and concurrent capacity/overlap protection.
- `npm run lint` and `npm run build`: PASS.
- Live packaged backend + Chromium: save/reload/submit, evaluator draft privacy/release, result display, publish/book/cancel, candidate/evaluator dashboards, loading/network retry, cross-role redirect and TRADE layout. Desktop and 375px mobile PASS with no runtime errors or horizontal overflow; screenshots visually reviewed.
- Updated API contracts, backend demo setup/environment example and decision D016. No dependencies added; existing uncommitted work preserved. Validation used temporary JDK 21/Node 24 and isolated ports 18085/15175. MySQL was not exercised. M5 has no remaining blockers.

## M6.1–M6.5 Validation — 2026-09-14

- Added mobile-first three-step TRADE onboarding with Bangla category cards, profile details, progress, review/save, existing skill/profile API integration and Bangla phone-digit normalization.
- Added optional `bn-BD` Web Speech recognition with feature/secure-context detection, microphone/listening control, editable transcript preview and explicit apply, navigation cleanup and manual fallback for permission/network/no-speech/microphone/language failures. No audio is stored by the application; browser speech processing may use an online service.
- Added candidate verification submission/status history, evaluator pending/detail/confirmed final decisions (VERIFIED/FAILED/FLAGGED), private notes and resubmission after FAILED/FLAGGED. Candidate/record locks prevent duplicate submissions and conflicting decisions. Candidate history and employer views exclude evidence/internal notes.
- Added shared `QueueEligibilityService` selection/admission hooks and actionable readiness cards. Requires latest verification, relevant released HIRE_READY evaluation, active TRADE skill/category/account, availability and no reservation/incompatible placement; admission also checks duplicate membership. Actual queue operations remain M7.
- `./mvnw package`: PASS; 60 tests, no failures/errors/skips, including five new integration tests for authorization, privacy, validation, finality, history, concurrent submissions/reviews and eligibility rules. Corrected stale/detached entities in the new test fixtures during validation.
- `npm run lint` and `npm run build`: PASS. Packaged dev backend health HTTP 200.
- Live Chromium desktop/375px mobile: category/profile/review/save/reload, manual unsupported-browser completion, deterministic speech-event simulation (bn-BD, listening, transcript edit/apply, cleanup and five error paths), flagged/resubmit/verify, private notes, released assessment → readiness, loading/retry and role redirects: PASS. No runtime errors or horizontal overflow; screenshots visually reviewed.
- Updated API contracts and decision D017. Existing uncommitted work preserved; no dependencies added. Validation used temporary JDK 21/Node 24, isolated ports 18086/15176. Real microphone transcription/browser speech-provider accuracy and live MySQL were not exercised. No implementation blockers remain for M6.

## M7.1–M7.5 Validation — 2026-09-14

- Implemented eligible TRADE queue admission/history/withdrawal, per-skill FIFO positions, stable ID tie-breaks, duplicate prevention, atomic cross-skill reservations and eligible release preserving join time.
- Added employer hiring from owned job shortlists, ACTIVE/COMPLETED/TERMINATED/REPLACED lifecycle, candidate/employer placement views and server-controlled 30-day managed TRADE coverage. Active replacement requests prevent conflicting placement termination/completion.
- Implemented Spring singleton `ReplacementQueueManager`: ownership/coverage validation, 24-hour target, FIFO eligibility rechecks, empty-queue failure, employer agreement confirmation, atomic activation, cancellation and retry. Changed eligibility releases/rematches; retries preserve original timestamps. Completed SLA is persisted ON_TIME/BREACHED; overdue unfinished responses derive BREACHED from server time.
- Added synchronous Spring event notification/audit observers with transaction rollback, owner-scoped notifications/read/read-all APIs and notification center. No external messaging or private evidence in employer DTOs/audit events.
- Added responsive replacement request/confirmation dialogs, countdown/progress, timeline, selected worker cards, coverage/status summaries, Bangla candidate queue and admin waiting room. Integrated hiring and role navigation into existing screens.
- `./mvnw package`: PASS; 69 tests, no failures/errors/skips. Nine new integration tests cover admission/ownership/positions, hiring transitions, FIFO ties/ineligible skipping, duplicate concurrent requests, concurrent cross-skill reservation, coverage/roles/privacy, observers/rollback, changed eligibility/cancellation and on-time/breached retry. Normalized generated timestamps to milliseconds after testing exposed response/database precision differences.
- `npm run lint` and `npm run build`: PASS. Packaged dev backend health HTTP 200.
- Live Chromium at 1440px/375px: existing signup/profile/assessment/verification setup → shortlist hire → queue join → replacement request/select/confirm/activate → ON_TIME → candidate placement/notifications/read-all. Loading/retry and role redirects PASS. No horizontal overflow/runtime errors; desktop/mobile screenshots visually reviewed.
- Updated API contracts and decision D018. Existing uncommitted work preserved; no dependencies added. Validation used temporary JDK 21/Node 24 and isolated ports 18087/15177. Live MySQL was not exercised. No M7 implementation blockers remain.

## M8.1–M8.5 Validation — 2026-09-14

- Added the training catalog, evaluator/admin referrals from released NEEDS_TRAINING evaluations, candidate referral view and transactional notification/audit. Enforces evaluator ownership, active accounts/programs, track compatibility, duplicate prevention and server-owned status.
- Added essential admin stats, safe user/job/skill/verification lists and confirmed account status changes. Protects admin accounts, audits changes and enforces inactive status against existing tokens. Existing placement/replacement/FIFO screens are linked into the console.
- Replaced the foundation landing page with dual-track CTAs, voice accessibility, manual verification and managed replacement explanations. Added dashboard activity/referral/notification cards, shared native confirmation dialog, compact keyboard-accessible mobile navigation, route titles/focus, reduced-motion support and lazy loading of admin/training screens. Training notifications link to Training.
- Added opt-in dev-only atomic fictional showcase data: 12 accounts, two companies, eight candidates, four jobs, skills, assessments/results, verifications, three FIFO entries, two placements, a selected replacement, notifications, a training referral and an appointment slot. Credentials come only from a private environment password. Relative timestamps keep the demo actionable; repeated initialization preserves edits.
- `./mvnw package`: PASS; 73 tests, zero failures/errors/skips. Four new tests cover referral authorization/release/privacy/duplicates/notification, inactive programs/client authority, admin status revocation/restoration/audit, and coherent/idempotent seeding with successful replacement acceptance/activation. Corrected a new test fixture that lacked an employer profile before final validation.
- `npm run lint` and `npm run build`: PASS, no warnings. Packaged dev backend health HTTP 200.
- Live Chromium 1440px/375px: landing and track/account CTAs; admin lists, status confirm/Escape/restore; evaluator referral → candidate view/notification/deep link; seeded replacement confirm/activate/ON_TIME; TRADE onboarding/dashboard; loading/error/retry/empty states; role redirects; mobile menu keyboard navigation and UI sign-in. PASS, no horizontal overflow/runtime errors; screenshots visually reviewed.
- Updated API contracts, decision D019, backend demo setup/environment example and progress. Existing uncommitted work preserved; no dependencies added. Validation used temporary JDK 21/Node 24 and isolated ports 18088/15178. Live MySQL and real microphone accuracy were not exercised. No M8 blockers remain.

## Update Rule

Codex updates only completed modules after their acceptance criteria/checks pass.
