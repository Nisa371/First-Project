# Revised Decisions

## D001 — University Showcase Scope

**Decision:** Build a showcase-quality MVP rather than a production-grade platform.

**Reason:** Limited time/Codex allowance and university project goals.

**Implication:** Visible UX, core workflows and replacement engine receive priority over enterprise infrastructure.

## D002 — Keep Two Candidate Tracks

TECH and TRADE remain central.

## D003 — Keep Four Authentication Roles

CANDIDATE, EMPLOYER, EVALUATOR, ADMIN.

TECH/TRADE remain candidate types.

## D004 — Simplify to One Primary Role Per User

**Decision:** The revised MVP does not require many-to-many user roles.

**Reason:** No showcase workflow needs multi-role accounts.

**Implication:** User may store a role enum. This reduces schema/auth complexity.

## D005 — Keep Spring Boot + React

Backend:
- Java/Spring Boot/Maven.

Frontend:
- React/TypeScript/Vite/Tailwind.

## D006 — H2 Is Valid for Demo

H2 may be used for development/tests/demo. MySQL compatibility remains desirable.

The project show does not require production DB infrastructure.

## D007 — JWT Without Refresh Tokens

A simple access-token JWT system is enough.

## D008 — Voice Is Client Accessibility

Use browser Web Speech API with manual fallback.

No biometric/audio-auth system.

## D009 — Manual Verification

Verification is a platform/manual workflow.

No fake NID/government integration.

## D010 — FIFO Replacement Remains Hero Feature

FIFO among currently eligible trade candidates remains the core algorithm.

## D011 — Preserve Four Academic Patterns

Strategy, Factory, Singleton-through-Spring, Observer-through-events remain required.

## D012 — In-App Notifications Only

No external SMS/email/voice provider required.

## D013 — Simplify Data Model

Remove low-value table proliferation:
- no role join tables;
- no mandatory voice transcript table;
- simplified assessment answer persistence;
- simplified training provider model.

## D014 — UI Quality Is Non-Negotiable

Scope reduction must not create a rough-looking CRUD project.

## D015 — Deployment Is Stretch

Reliable local demo is sufficient. Simple deployment may be added after the project is complete if credits/time remain.

## D016 — M5 Assessment and Booking Boundaries

The seeded demo uses one resumable, untimed attempt per candidate/assessment. TECH Strategy computes a weighted percentage; TRADE/Voice Strategy defers scoring to the evaluator because MCQ responses alone do not demonstrate practical skills. Voice capture remains M6. A saved evaluation belongs to its first evaluator and stays private until explicit, final release. This keeps the review workflow demonstrable without retake policy or assessment authoring infrastructure.

Booking uses database row locks on candidate and slot, and evaluator slot creation locks the evaluator user. Half-open time intervals permit adjacent appointments; timestamps are normalized to milliseconds before comparison/persistence. Capacity applies to each slot; cancellation frees a place. Completion/no-show administration is outside this M5 booking scope.

## D017 — M6 Verification, Voice and Readiness

Onboarding saves through existing profile and skill APIs. Bangla (`bn-BD`) Web Speech input is optional, starts only on a microphone action, and requires transcript preview/edit/apply. The app persists only confirmed profile text, never audio; the browser may process speech online. Unsupported browsers and permission/network/microphone/language errors retain manual completion.

Verification uses a private identity reference with manual platform review, not government NID integration. Candidate history excludes raw evidence and internal reviewer notes. Decisions are final; FAILED/FLAGGED allow a new submission with preserved history. First final reviewer wins under a record lock. A separate claimed-review workflow is unnecessary for this MVP; legacy IN_REVIEW records remain reviewable.

Queue eligibility requires a released HIRE_READY TRADE evaluation. General TRADE assessments apply across trades; skill-specific assessments must match an active profile skill (and the requested queue skill). The newest relevant assessment attempt with a released evaluation wins, with evaluation ID as tie-breaker. Unreleased scores never establish readiness. PENDING and ACTIVE placements block eligibility. Actual queue admission, FIFO and reservation orchestration remain M7; readiness does not imply queue membership.

## D018 — M7 Managed Placement and Replacement

Hiring from an owned shortlist immediately activates the placement. Verified, hire-ready TRADE placements receive a server-controlled 30-day guarantee; TECH placements have no replacement guarantee. Employer confirmation represents both-party agreement for the showcase. Replacement activation marks the original REPLACED and starts the new placement's own coverage.

Explicit eligible candidate queue admission uses join time and stable ID, never scores. A Spring singleton queue manager uses database locks; a stable first skill-catalog row serializes replacement mutations across skills at MVP scale, followed by placement/request and candidate locks. This intentionally favors a small, deterministic demonstration over high-throughput matching. Candidate profile/skill changes share candidate locks with queue/hiring checks. No static in-memory reservation state is used.

Empty queues persist FAILED, with explicit retry preserving the original 24-hour deadline. Unavailable selections are released/rematched on confirmation or activation; cancelled eligible reservations keep their join time. Overdue reads derive BREACHED without a scheduler, while completion persists the final SLA result. Synchronous Spring event observers write notifications and allowlisted audit records in the workflow transaction so all roll back together. No external messages are sent.

### D019 — Training, small admin console and opt-in showcase data

Referrals require a released NEEDS_TRAINING evaluation; evaluators can refer from
their own reviews and admins from any eligible review. Candidate locking prevents
duplicate active program referrals. In-app notification/audit observers share the
transaction. The MVP has no enrollment or referral-status editing workflow.

Admin status changes protect all admin accounts to prevent showcase lockout;
roles cannot be edited. Overview DTOs exclude verification evidence and private
notes. Lists remain simple for university-scale data.

The rich fixture is dev-only, explicitly enabled and requires an environment
password. One transaction creates the deterministic fictional graph, with
relative timestamps for usable coverage/SLA/appointments. A committed marker
prevents overwriting subsequent edits. It is not a production migration.
