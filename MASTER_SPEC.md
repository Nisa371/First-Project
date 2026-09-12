# Verified Skill & Career Managed Marketplace with Voice-First Accessibility

> Master project specification — permanent source of truth.
> Version: 1.1 | Latest module: M0.2 | Date: 2026-09-12
> Repository: Nisa371/First-Project
> Implementation status: specification only; no application features implemented.

This document defines the approved initial direction for future modules. Requirements describe the eventual system, not currently available functionality. Planning-level entities, statuses, and package names may be refined through explicitly documented decisions. Future prompts that conflict with this specification must be identified before architectural changes are made.

## 1. Project overview

A two-tier managed employment marketplace primarily for Bangladesh, connecting job candidates, B2B employers, evaluators/experts, training institutes, and platform administrators. The platform evaluates and shortlists candidates so employers need not manually filter large volumes of unverified CVs.

### Tier 1 — Corporate & Tech Track

For CSE students, software developers, engineers, university graduates, and corporate professionals.

Candidate registration → profile completion → CV/portfolio submission → skill assessment → evaluator review → employer shortlisting → interview/consultation → placement.

### Tier 2 — Trade & Field Track

For drivers, electricians, AC technicians, mechanics, delivery riders, plumbers, and other skilled or semi-skilled workers, including people with limited digital literacy.

Trade worker registration → trade/service selection → Bangla voice-assisted information entry → appointment/consultation → identity/background/ethics verification → skill evaluation → verified candidate pool → waiting list → employer placement.

The Bangla voice-first interface minimizes typing. Browser-native Web Speech API recognition may support the MVP where available. Equivalent manual forms must always remain available. Speech input is an accessibility aid, not biometric authentication or independent verification evidence.

## 2. Core business model

Basic access to employment opportunities must not require candidate payment. Revenue is primarily B2B.

| Revenue source | Payer and value |
| --- | --- |
| Corporate Placement Fee | Employers pay upon successful hiring of qualified corporate/technical candidates. |
| Verified Workforce Service Fee | Employers pay for hiring verified trade workers, including verification and an eligible replacement guarantee. |
| Training Institute Referral Commission | Partner institutes may provide a commission for appropriate training referrals. |

Fees, commission rates, and commercial terms remain to be defined. Training recommendations should address skill gaps, not guarantee employment. Payment processing is not required for the initial MVP unless explicitly authorized in a later module.

## 3. Core differentiator — 24-Hour Replacement Guarantee

Eligible trade-worker placements support a replacement request if the worker leaves or becomes unavailable during the defined guarantee period. Skill-specific waiting lists provide verified replacement candidates.

The **24-hour replacement response/fulfillment target** and the **duration of placement guarantee coverage** are separate concepts. Coverage duration, the precise clock start, what counts as fulfillment, exclusions, and escalation policy are open business decisions; this specification does not invent contractual terms or claim the MVP can guarantee real-world fulfillment.

Example: an AC Technician Queue contains Candidate A, Candidate B, then Candidate C. Selection considers the next *eligible* worker, not blindly the first record.

### Replacement process

1. Employer submits a request for a placement they own.
2. The system validates coverage, reason, ownership, and whether a replacement is already in progress.
3. Determine the required skill and applicable job constraints.
4. The queue manager searches the skill-specific waiting list.
5. Select the next eligible verified, active, available worker.
6. Atomically reserve the candidate and record the selection; contact is manual or represented by a clearly labeled notification workflow in the MVP.
7. Notify the employer/candidate without exposing unnecessary identity evidence.
8. Track acceptance and create/link the replacement placement when confirmed.
9. Record notifications and audit events throughout the workflow.
10. Mark completion only after the defined completion condition is satisfied.

### Queue and failure rules

- Proposed MVP ordering is FIFO by queue-entry time, with a stable identifier as a tie-breaker, among eligible candidates. This is an initial assumption subject to approval before implementation.
- Recheck verification, account status, availability, and reservation status at selection time.
- A candidate may have multiple skills, but must not be reserved or actively placed incompatibly across queues.
- Reservation/placement changes must be transactional; concurrent requests must not assign the same worker twice. A singleton service alone does not provide this guarantee.
- Repeated requests must not create duplicate active replacements for one incident/placement.
- Record skipped/ineligible candidates, reservation release, and failed selection with appropriate operational context, without logging sensitive evidence.
- If no candidate is available, retain an explicit failure/unfulfilled result and notify administrators; do not fabricate a successful replacement.
- Decline, timeout, cancellation, retry, and escalation policies must be finalized in the replacement module. Keep attempts/history rather than silently erasing failure.
- Store relevant request, selection, notification, and completion timestamps so the eventual 24-hour policy can be measured. A missed target must be visible, not treated as success.
- Replacement records link the original placement, request, selected candidate, and eventual replacement placement.

Real SMS and automated calling are not required to model this workflow correctly.

## 4. User roles and access boundaries

| Role | Responsibilities and boundaries |
| --- | --- |
| CANDIDATE | Manage their own profile, skills, submissions, assessments, bookings, results, and referrals; participate in hiring/placement workflows. |
| EMPLOYER | Manage their organization profile and jobs/workforce requirements; discover authorized candidate information; shortlist, arrange interviews, hire, manage owned placements, and request eligible replacements. |
| EVALUATOR | Review assigned submissions/assessments and permitted verification information; interview candidates; score, write notes, and recommend hiring or training. |
| ADMIN | Manage users, employers, evaluators, training institutes, skills/categories, verification review, assessments, partners, dashboards/analytics, and audit access. |

Candidate type is TECH or TRADE on a shared account foundation. Do not create separate authentication systems for the tracks. Account status, candidate type, verification status, and assessment recommendation are separate concepts.

Training institutes are partner entities managed through administration in the initial scope; a separate institute login role is not required unless later approved. Privileged roles must not be self-assigned through public registration. Employer approval criteria and role-assignment procedures are decisions for the relevant modules.

### Canonical roles and candidate types

Primary roles: CANDIDATE, EMPLOYER, EVALUATOR, ADMIN.
Candidate types: TECH, TRADE.

> Candidate type is domain classification, not an authentication role.

Both tracks use CANDIDATE and the shared account foundation. The user_roles model may support multiple memberships, while normal MVP onboarding assigns one operational role.

### Core authorization principles

Apply least privilege, backend enforcement, resource ownership/assignment, sensitive-data restrictions, and privileged role assignment. Client-supplied IDs never establish ownership. Account status is independent of role; normal protected actions require ACTIVE. Employers receive safe candidate projections, evaluators receive assigned/relevant data, and administrative operations remain validated and audited. Queue order remains under backend business logic.

The detailed authorization specification, including the permission matrix, conditional rules, role/type changes, and security invariants, is [docs/ROLES_AND_PERMISSIONS.md](docs/ROLES_AND_PERMISSIONS.md). M0.2 defines behavior only; no security implementation is present.

## 5. Primary user workflows

### 5.1 Tech candidate

Register → login → complete profile → add skills → upload CV/portfolio → take assessment → evaluator reviews → receive evaluation → employer discovers qualified candidate → shortlist → interview/consultation → hire → placement record.

### 5.2 Trade candidate

Register → select TRADE → choose service/skill category → enter information manually or through Bangla speech → book appointment when necessary → submit verification information → evaluator/admin reviews identity/background/ethics information and skill evaluation → become VERIFIED → enter appropriate waiting list → employer hires → placement record.

Unverified, failed, flagged, suspended, blocked, or unavailable candidates must not be presented as currently eligible verified replacement workers. Verification is an explicit review outcome, not the mere presence of a submitted document.

### 5.3 Employer

Register → employer profile setup → approval/verification if required → create job/workforce requirement → search qualified candidates → view permitted profiles → shortlist → request interview/consultation → hire → manage placement → request replacement if eligible.

### 5.4 Assessment

Assessment created → candidate starts → answers questions or supplies a submission → attempt/answers stored → automatic scoring where appropriate → evaluator review where necessary → final evaluation → candidate result → hiring or training recommendation.

A voice-assisted submission is not automatically proof of competence. Preserve the distinction between automated score and evaluator-approved result.

### 5.5 Replacement

Worker placed → worker becomes unavailable within coverage → employer submits request → eligibility validated → required skill resolved → waiting list queried → next eligible worker reserved → replacement record/selection recorded → notifications and audit events → acceptance → linked replacement placement → completion.

Invalid requests must be rejected with a clear business-rule explanation. Queue exhaustion must remain an explicit unsuccessful outcome.

### 5.6 Training referral

Assessment/evaluation → skill gap identified → NEEDS_TRAINING recommendation → suitable programs shown → referral to partner institute → referral tracked.

Referral states: REFERRED, CONTACTED, ENROLLED, COMPLETED, CANCELLED. Referral completion does not automatically verify a worker or guarantee hiring.

## 6. Initial technology stack

| Layer | Initial decisions |
| --- | --- |
| Frontend | React, TypeScript, Vite, Tailwind CSS, React Router, Axios |
| Optional later frontend additions | TanStack Query, React Hook Form, Zod — not mandatory dependencies |
| Backend | Java, Spring Boot, Maven, Spring Web, Spring Data JPA, Spring Security, Bean Validation, Lombok, JWT-based authentication |
| Main relational database | MySQL |
| Development/testing database | H2 may be used where convenient; important behavior must also be verified against MySQL |
| Voice | Browser Web Speech API with Bangla recognition where supported; permanent manual text-entry fallback |
| Expected development tools | Git, GitHub, VS Code, IntelliJ IDEA, Postman |

Exact versions, supported browser matrix, JWT lifecycle, and deployment configuration are to be selected in their implementation modules. No dependencies or configuration files are created in M0.1.

## 7. High-level architecture

A modular monolith is appropriate for the academic MVP.

React + TypeScript frontend → REST API → Java Spring Boot backend → domain/business services → Spring Data JPA → MySQL.

| Integration | Responsibility |
| --- | --- |
| Web Speech API → frontend voice workflow | Optional speech-to-text input, editable before submission |
| Domain notification events → notification system | User-facing workflow notifications |
| Waiting lists → replacement queue service | Eligibility, ordering, reservation, selection, release |
| Training institutes → referral module | Programs, recommendations, referral tracking |

Controllers handle transport/validation and delegate business behavior to services. Domain services enforce invariants and transactions; repositories handle persistence. Access checks must include both role and affected-record ownership/assignment.

## 8. Planned backend modules

Proposed package root: `backend/src/main/java/com/marketplace/`.

| Package | Planned responsibility |
| --- | --- |
| config | Application and security configuration |
| common | Shared errors, DTO conventions, validation helpers |
| auth | Registration, login, JWT/security flow |
| user | Accounts, roles, account status |
| candidate | Shared candidate profiles, TECH/TRADE type, skills, CV metadata |
| employer | Employer organization profiles and approval |
| job | Jobs/workforce requirements and shortlisting |
| assessment | Questions, attempts, answers, evaluation and recommendations |
| appointment | Interview slots, consultations, bookings |
| voice | Optional voice-submission metadata support, not biometric authentication |
| verification | Restricted evidence references, reviews, verification outcomes |
| placement | Hiring, placements, replacement request lifecycle |
| queue | Skill waiting lists and replacement reservation/selection |
| referral | Institutes, programs, referrals |
| notification | Workflow notifications and delivery state |
| audit | Restricted business-action audit records |
| dashboard | Role-appropriate summaries and admin analytics |

Exact naming may evolve without losing feature boundaries. A frontend-only speech capability does not require a server audio-processing subsystem.

## 9. Planned frontend modules

Proposed root: `frontend/src/`.

Shared areas: `app`, `pages`, `components`, `layouts`, `services`, `hooks`, `types`, `router`.

Feature areas: `auth`, `candidates`, `employers`, `assessments`, `appointments`, `voice`, `verification`, `placements`, `replacements`, `referrals`, `notifications`, `admin`, `dashboard`.

Prefer feature-based organization, with feature-specific components/services/types colocated, instead of one large component directory. Frontend route/role checks improve UX but never replace API authorization.

## 10. High-level data model

These are conceptual entities/tables, not final schema, migrations, or Java classes.

| Domain | Likely tables | Main relationships/purpose |
| --- | --- | --- |
| Authentication | users, roles, user_roles | Shared accounts; user-role membership |
| Candidate | candidate_profiles, skills, candidate_skills | Candidate profile linked to user; candidate-skill association |
| Employer | employer_profiles, jobs | Employer profile linked to account; jobs owned by employer |
| Assessment | assessments, assessment_questions, assessment_attempts, assessment_answers, evaluation_results | Assessment questions; candidate attempts/answers; reviewer results |
| Appointments | interview_slots, bookings | Available slots and participant bookings |
| Trade/verification | voice_registrations, verification_records | Optional transcription/metadata and restricted candidate verification reviews |
| Placement/replacement | placements, waiting_list_entries, replacement_requests | Candidate/employer/job placement; skill queue eligibility/reservation; original and replacement placement linkage |
| Training | training_institutes, training_programs, referrals | Institute programs and candidate referrals |
| Platform | notifications, audit_logs | Recipient notifications and actor/entity/action history |

Schema planning must preserve these relationships:

- Candidate profiles reference shared users and TECH/TRADE type.
- Candidate skills associate candidates with reusable skill categories.
- Attempts reference a candidate and assessment; answers reference an attempt and question where applicable.
- Evaluation results retain evaluator/strategy context and recommendation.
- Verification records remain separate from publicly visible profile projections.
- Placements identify employer, candidate, and the relevant requirement, with lifecycle timestamps and applicable guarantee policy.
- Waiting-list entries identify candidate, skill, eligibility/order, and reservation state.
- Replacement requests retain the original placement and eventual replacement linkage.
- Referrals reference candidate, institute/program, and relevant evaluation when applicable.
- Notifications identify recipient and event/entity; audit records identify actor, action, entity, and time.

Shortlist persistence, employer organization membership, reservation history, file storage, and detailed status-history modeling must be resolved during schema design; supporting tables may be approved then. Do not omit required workflow persistence merely because this initial table list is non-exhaustive.

## 11. Initial conceptual statuses

These are planning vocabulary, not final Java enums.

| Concept | Statuses |
| --- | --- |
| Candidate verification | PENDING, IN_REVIEW, VERIFIED, FAILED, FLAGGED |
| Account | ACTIVE, SUSPENDED, BLOCKED, FLAGGED |
| Assessment recommendation | HIRE_READY, NEEDS_TRAINING, REJECTED |
| Placement | PENDING, ACTIVE, COMPLETED, TERMINATED, REPLACEMENT_REQUESTED, REPLACED |
| Replacement | REQUESTED, MATCHING, CANDIDATE_SELECTED, EMPLOYER_NOTIFIED, ACCEPTED, COMPLETED, FAILED |
| Referral | REFERRED, CONTACTED, ENROLLED, COMPLETED, CANCELLED |

Typical verification progression is PENDING → IN_REVIEW → VERIFIED or FAILED; FLAGGED requires authorized review. Typical replacement success progresses REQUESTED → MATCHING → CANDIDATE_SELECTED → EMPLOYER_NOTIFIED → ACCEPTED → COMPLETED; unsuccessful processing must record FAILED and its reason where appropriate.

The placement being replaced must retain its history; the new placement is separately linked. Notification does not imply acceptance, and selection does not imply completion. Final transition matrices, retry/review paths, waiting-list states, cancellation, and reservation-expiry behavior belong to the relevant modules. Status transitions require authorization and business-rule validation.

## 12. Required object-oriented design patterns

This is an academic Java/OOP project. Patterns must solve real domain problems, not be added artificially.

| Pattern | Intended use | Design constraint |
| --- | --- | --- |
| Strategy | AssessmentStrategy with potential TechAssessmentStrategy and VoiceAssessmentStrategy | Vary evaluation behavior by assessment/candidate context; speech recognition is not a competency or identity verdict. |
| Factory | CandidateFactory or AssessmentStrategyFactory where creation genuinely varies | Choose the specific implementation during the relevant module; avoid factories for trivial construction. |
| Singleton | ReplacementQueueManager as a normal Spring singleton-scoped service | One managed service instance per application context satisfies the centralized lifecycle requirement; no unsafe static global mutable queue. |
| Observer | Domain events with NotificationListener, AuditLogListener, QueueListener | Spring application events may decouple reactions to verified candidates, shortlists, placements, replacement requests, and referrals. |

Queue state must be persisted. Database transactions/concurrency controls protect reservations even with multiple application instances. A Spring singleton is not a cluster-wide singleton.

Observers must not publish successful user-facing results for rolled-back changes. Define after-commit handling or transactional persistence appropriately; listeners must tolerate duplicate processing. Durable distributed event infrastructure is not an initial requirement.

## 13. Security principles

- Never store plaintext passwords; use an appropriate password-hashing mechanism through Spring Security.
- Use Spring Security for authentication and backend roles/permissions plus ownership/assignment checks for authorization.
- JWT secrets must come from environment variables; never commit database credentials, passwords, tokens, or signing keys.
- Validate incoming data server-side with Bean Validation and service-level business checks.
- Frontend role checks are for UX only.
- Do not expose sensitive verification information through public candidate APIs.
- Validate upload size, filename, extension, and MIME type. Treat uploads as untrusted, authorize downloads, and define safe storage during the upload module.
- Audit important employer/admin actions and security-relevant events without passwords or secrets.
- Deny privileged self-registration/escalation; define approved role provisioning.
- Authentication modules must explicitly decide token expiry/storage/revocation and relevant CORS/CSRF controls; do not assume JWT eliminates browser security risks.

## 14. Privacy and verification rules

Potential sensitive data includes NID-related verification information, contact information, verification notes, voice transcription/metadata, and emergency contacts.

1. Restrict sensitive verification evidence to authorized roles and assignments.
2. Employers receive only necessary candidate information, not unrestricted identity evidence.
3. Platform verification means a platform review outcome; do not claim government validation without a real authorized integration.
4. Development and demos must never use real people's sensitive identity data.
5. All seed identities must be fictional.
6. Minimize collected data, especially raw audio and NID images; the MVP need not retain raw audio merely to support speech input.
7. Explain microphone use, obtain user permission, allow correction of transcription, and retain manual entry after denial or failure.
8. Browser/provider processing behavior, retention/deletion policy, consent handling, and production privacy review must be resolved before collecting real sensitive data.

This specification makes no claim of legal compliance or authorized government connectivity.

## 15. Initial MVP scope

Prioritize working end-to-end workflows over external integrations.

- Authentication and role-based access control.
- Candidate and employer profiles.
- Jobs and candidate skills.
- CV metadata/upload workflow.
- Assessments and evaluator workflow.
- Appointment booking.
- Trade-worker onboarding and Bangla voice-input prototype with manual fallback.
- Verification records.
- Waiting lists.
- Placements and replacement requests.
- Notifications.
- Training institute referrals.
- Admin dashboard.
- Audit logs.

M0.1 implements none of these features; it only records the scope.

## 16. Future/V2 — not required initially

- Real government NID API integration.
- Production SMS gateway.
- Production voice-call gateway.
- AI-powered CV scoring.
- AI interview evaluation.
- Payment gateway.
- Automated invoicing.
- Native Android/iOS application.
- Advanced recommendation engine.
- Full biometric voice authentication.
- Complex real-time messaging.
- Microservices.
- Kubernetes.
- Large-scale distributed infrastructure.

Future expansion must not unnecessarily complicate the academic MVP. Simulated integrations must be clearly labeled; an in-app notification is not proof that an SMS or call occurred.

## 17. UI/UX principles

The general interface must be modern, professional, responsive, mobile-friendly, accessible, and consistent. Corporate users may use standard dashboards.

Trade-worker journeys prioritize Bangla language, large buttons, clear icons with understandable labels, minimal text/typing, simple navigation, obvious progress indicators, voice input, permanent manual fallback, and mobile-first layout.

Provide visible recording/listening/error states, user-controlled start/stop, editable transcripts, and confirmation before submission. Do not silently submit recognized speech. Keyboard navigation, labels, focus visibility, readable contrast, and non-audio-only feedback remain important for both tracks.

## 18. API design principles

REST APIs use an `/api/...` prefix.

Initial resource families: `/api/auth`, `/api/candidates`, `/api/employers`, `/api/jobs`, `/api/assessments`, `/api/bookings`, `/api/verification`, `/api/placements`, `/api/replacements`, `/api/referrals`, `/api/notifications`, `/api/admin`.

Use correct HTTP methods/status codes, explicit request/response DTOs, validation, consistent errors, and pagination where needed. JPA entities must not be exposed directly by controllers. List/search responses must obey authorization and privacy filtering.

Exact endpoint contracts belong to implementation modules. State-changing placement/replacement operations must account for retries and concurrency.

## 19. Error handling

Use centralized backend exception handling. Conceptual response:

```json
{
  "timestamp": "ISO_TIMESTAMP",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Human-readable explanation",
  "path": "/api/example"
}
```

Categories include resource not found, validation error, authentication error, authorization error, business-rule violation, conflict, and unexpected server error.

Choose suitable HTTP codes, such as 400 for invalid input, 401 for missing/invalid authentication, 403 for forbidden operations, 404 for missing resources, 409 for state conflicts, and 500 for unexpected failures. Exact business-error mapping is a later API decision. Do not return stack traces, secrets, or sensitive verification evidence.

## 20. Audit requirements

Important events include LOGIN, PROFILE_UPDATED, JOB_CREATED, ASSESSMENT_SUBMITTED, EVALUATION_COMPLETED, CANDIDATE_VERIFIED, CANDIDATE_FLAGGED, CANDIDATE_SHORTLISTED, PLACEMENT_CREATED, REPLACEMENT_REQUESTED, REPLACEMENT_COMPLETED, and REFERRAL_CREATED.

Records must identify who acted, what happened, the affected entity, and timestamp. Include outcome and safe contextual identifiers where useful. System-originated events identify a system actor. Keep access restricted and prevent ordinary users from rewriting audit history.

Never log passwords, JWTs, signing keys, raw identity evidence, or unnecessary personal data. Audits must reflect committed outcomes, not fictional success from failed operations.

## 21. Testing strategy

### Backend unit tests

Prioritize authentication service, assessment strategies/scoring, placement service, replacement queue service, and referral service. Test valid behavior, invalid transitions, missing/ineligible records, and failure paths.

### Integration tests

- Registration → Login.
- Candidate → Assessment → Evaluation.
- Employer → Job → Shortlisting.
- Verification → Waiting List.
- Candidate → Placement.
- Replacement Request → Replacement Candidate.
- Failed/weak Assessment → Training Referral.

Test reservation concurrency, duplicate replacement requests, empty queues, guarantee boundary timestamps, notification/audit behavior, and rollback consistency. Use controlled clocks for time-sensitive policy tests. H2 tests alone must not be assumed to prove MySQL locking or SQL behavior.

### Security tests

Candidates cannot access admin endpoints. Employers cannot access evaluator-only endpoints. Users cannot access another user's private data or modify another employer's placements. Employers must not receive restricted verification evidence. Public registration cannot grant privileged roles.

### Frontend/manual acceptance tests

Verify Bangla input where supported, unsupported-browser fallback, denied microphone permission, recognition failure, transcript correction, mobile usability, keyboard access, and server validation feedback. No external integration may be reported as real unless actually configured and verified.

## 22. Final academic demo requirements

Use fictional identities and enough verified workers in the same skill category to show an original placement and a different replacement.

1. Register a trade worker.
2. Complete the trade-worker profile.
3. Verify the worker.
4. Add the worker to a waiting list.
5. Employer creates a workforce requirement.
6. Employer hires a worker.
7. Create a placement.
8. Record worker unavailability.
9. Employer submits an eligible replacement request.
10. Queue manager selects the next verified eligible candidate.
11. Create/confirm the replacement and linked placement.
12. Show notifications.
13. Show audit records for the actions.

Demonstrate authentication, role management, candidate/employer functionality, verification, placement, queue selection, replacement logic, Observer/event behavior, and audit logging. Also show an empty-queue or ineligible-request outcome without pretending success. Demo-specific guarantee values must be labeled fictional/configurable, not final commercial policy.

## 23. Codex Development Rules

1. Always inspect existing code before editing.
2. Do not rewrite unrelated modules.
3. Build only the requested module.
4. Reuse existing project conventions.
5. Do not introduce dependencies unnecessarily.
6. Do not silently change the architecture.
7. Never hard-code passwords, secrets, database credentials, or JWT keys.
8. Keep controllers thin.
9. Put business logic in services/domain services.
10. Use DTOs for REST input/output.
11. Validate incoming data.
12. Write maintainable, readable code.
13. Add tests for important business logic.
14. Update MASTER_SPEC.md when an approved architectural decision changes.
15. Do not implement mock external integrations as if they were real production services.
16. Preserve backwards compatibility with completed modules whenever practical.
17. If a future prompt conflicts with MASTER_SPEC.md, explicitly mention the conflict before making architectural changes.
18. Do not create duplicate implementations when an equivalent component already exists.
19. Prefer simple solutions suitable for an academic MVP before introducing large-scale architecture.
20. Every module should leave the repository in a runnable state.

For documentation-only M0.1, rule 20 means preserving any existing runnable application; it does not authorize creating an application scaffold. No application exists yet.

## 24. M0.1 definition of done

- MASTER_SPEC.md exists at repository root as structured Markdown.
- Project purpose, TECH/TRADE tracks, roles, and core workflows are recorded.
- The replacement guarantee and queue workflow are documented.
- The initial stack, frontend/backend architecture, and conceptual data model are recorded.
- MVP and future/V2 scope are clearly separated.
- OOP patterns, security/privacy rules, testing expectations, and Codex Development Rules are present.
- No frontend, backend, database, authentication, migration, or business feature code is implemented.
- Unrelated files remain untouched.
- Work stops after M0.1; no M0.2 work is authorized by this module.

## 25. Assumptions, open decisions, and inspection record

### Repository inspection

GitHub's repository contents endpoint reported that Nisa371/First-Project was empty before M0.1. Repository metadata identified main as the default branch. No README, existing MASTER_SPEC.md, documentation, frontend/backend folders, configuration files, or prior specifications existed to preserve or reconcile. No existing-documentation conflicts were found.

### Initial assumptions

- Use the stated Java/Spring Boot and React/TypeScript stack, not a stack inferred from unrelated projects.
- Use a modular monolith with shared candidate authentication.
- Institutes begin as admin-managed partners rather than a fifth authentication role.
- FIFO among eligible candidates is the proposed queue ordering.
- Model the 24-hour target separately from coverage duration; no production contractual policy is invented.
- The initial notification workflow may be in-app/manual, with external services explicitly deferred.

### Decisions required before affected implementation

- Guarantee duration, clock start, fulfillment definition, eligibility/exclusions, acceptance, timeouts, escalation, and treatment of replacement coverage.
- Queue ordering approval, availability matching, reservation expiry, decline/retry behavior, and final state transitions.
- Employer approval and organization membership; privileged role assignment.
- Assessment rubrics, thresholds, evaluator assignment, verification criteria, and review/appeal procedures.
- Supported runtime/library versions, JWT lifecycle, upload limits/storage, and deployment configuration.
- Privacy consent, data minimization, retention/deletion, and browser speech-processing disclosure.
- Detailed schema and API contracts, including shortlists and reservation/history persistence.

Future modules should resolve only the decisions they need, record approved changes here, and avoid silently treating unresolved policy as settled.

### Change history

| Version | Module | Change |
| --- | --- | --- |
| 1.0 | M0.1 | Initial master specification; documentation only. |
| 1.1 | M0.2 | Added canonical authorization summary and link to detailed roles/permissions specification; documentation only. |
