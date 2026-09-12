# System Architecture

> M0.5 | Version 1.0 | Planning only; no deployed or implemented system is claimed.

## 1. Authority and scope

[MASTER_SPEC.md](../MASTER_SPEC.md) governs product/architecture. Apply [development standards](DEVELOPMENT_STANDARDS.md), [roles](ROLES_AND_PERMISSIONS.md) and [workflows](BUSINESS_WORKFLOWS.md). [DATABASE.md](DATABASE.md) plans persistence, [API_SPEC.md](API_SPEC.md) plans contracts and [ROADMAP.md](ROADMAP.md) fixes implementation IDs/dependencies.

Use a modular monolith: React/TypeScript frontend, REST interface, Java/Spring Boot business application, Spring Data JPA, MySQL. H2 may support development/testing; MySQL is the main target. Versions, ID strategy and concrete infrastructure configuration remain deferred.

## 2. System context

| Actor | Interaction and boundary |
| --- | --- |
| TECH candidate | Shared CANDIDATE account; professional profile/CV, assessments, released results, interviews and hiring |
| TRADE candidate | Same CANDIDATE role; Bangla-assisted/manual input, verification, skill evaluation, queue eligibility, offers and placement |
| Employer | Own company/jobs/shortlists/placements; safe candidate discovery; eligible replacement requests |
| Evaluator | Assigned submissions/verification duties, scores, feedback and consultations; no self-review or unrelated evidence |
| Admin | Operational oversight, reference data, privileged role/status actions, safe analytics and audit inspection |
| Training institute | External business partner/program provider; admin-managed records and operational referrals; no MVP institute login |

The frontend is untrusted input. TECH/TRADE are profile types, not authentication roles. Normal protected operations require ACTIVE. Formal employer verification is optional/future; completed employer profile remains a workflow gate.

## 3. Layers and request flow

Browser UI → feature API module → shared Axios client → security filter/authentication → Spring controller/DTO validation → service ownership/assignment and business checks → repository → MySQL.

Responses return through authorized mapping to consumer-specific DTOs, then feature API/UI. Controllers remain thin; services orchestrate business operations and transactions; repositories perform persistence rather than workflows. Security error paths use the same approved error contract where applicable.

| Layer | Responsibilities |
| --- | --- |
| React UI/pages | Interaction, accessibility, presentation and safe client validation |
| API services | Central configuration, typed transport and consistent error handling |
| Security boundary | Authentication, current account status, coarse role gate |
| Controller | Request/response DTO and transport status |
| Application/domain services | Ownership/assignment, allowed transitions, eligibility, transactions and domain events |
| Repository/JPA | Scoped queries, storage and persistence constraints |
| MySQL | Relational integrity and concurrency-supported durable state |

No controller directly returns JPA entities. Frontend role routing is UX only. IDs, hidden controls, claimed roles and client-supplied statuses do not establish authority.

## 4. Frontend modules

Planned frontend/src/ shared areas: app/, pages/, components/, layouts/, hooks/, router/, services/, types/. Features live under features/ with auth, candidates, employers, jobs, assessments, appointments, voice, verification, placements, replacements, referrals, notifications, admin and dashboard. Preserve plural candidates/employers from the master/standards; illustrative singular paths are not new names.

Pages compose focused feature components. Feature hooks/API modules use the shared Axios client. Limited shared auth state may use Context; optional server-state/form libraries require justification. Typed requests/results match API DTOs and error/fieldErrors contract.

Every data-driven view handles loading, empty, success, validation and failure states. Trade workflows use mobile-first large controls, Bangla labels, transcript review and permanent manual input. Browser recognition can fail or be unavailable without blocking onboarding. No mandatory raw audio upload subsystem.

## 5. Backend modules

Root: backend/src/main/java/com/marketplace/ unless an explicitly approved implementation establishes another root.

| Feature | Boundary |
| --- | --- |
| config/common | Configuration, validation/error conventions and narrow shared utilities |
| auth/user | Registration/login, memberships, current status and trusted account operations |
| candidate | Shared candidate profile, type, skills, CV and completeness |
| employer/job | Company data, owned requirements, safe discovery and shortlist membership |
| assessment | Definitions/questions, immutable attempts, scoring strategies, assigned evaluation and release |
| appointment | Slots, booking participants/capacity, consultation/interview outcomes and notes |
| voice | Confirmed transcript/metadata support only where needed |
| verification | Restricted case evidence/review and authoritative current outcome |
| placement | Hiring agreements, placements, guarantee coverage and replacement-request lifecycle |
| queue | Eligibility/FIFO, candidate-wide claim, release and replacement selection |
| referral | Partner institutes/programs and referral states |
| notification | IN_APP records and intended delivery/read state |
| audit | Immutable attributable business/security evidence |
| dashboard | Scoped aggregates and operational presentation queries |

Each feature may contain controller, service, repository, dto, mapper, entity and event/exception areas when useful; do not force every subfolder into small features. Replacement orchestration spans placement and queue through explicit service contracts, not circular controllers or duplicated managers.

Readiness, verification, completeness and availability are separate sources. A query may combine their facts but cannot create a competing authoritative status field. Snapshot/cache projections require explicit invalidation if later introduced.

## 6. Required academic patterns

| Pattern | Conceptual implementation | Purpose/constraint |
| --- | --- | --- |
| Strategy | AssessmentStrategy; TechAssessmentStrategy and VoiceAssessmentStrategy where behavior differs | Avoid giant branching; human trade review is not AI scoring or voice identity proof |
| Factory | AssessmentStrategyFactory, or a justified candidate factory | Select genuine variable construction/strategy; no redundant factory for claims alone |
| Singleton | Spring-managed ReplacementQueueManager | One managed service per context; persisted queues and transactions, no static global mutable list |
| Observer | PlacementCreatedEvent, CandidateVerifiedEvent, ReplacementRequestedEvent, ReferralCreatedEvent | NotificationListener, AuditLogListener, QueueListener handle scoped reactions |

Conceptual class/event names are not implemented code or replacements for M0.3 audit identifiers. Map domain events to existing audit actions with correlation. No success notification on rollback, duplicate effects on replay, or recursive notification/audit loops.

## 7. Transaction and concurrency boundaries

| Operation | Consistent outcome |
| --- | --- |
| Booking | Capacity + participants + reservation committed once; no overbooking |
| Verification change | Authoritative outcome/history + eligibility effects + audit intent; no revoked worker selection |
| Waiting-list enrollment | One active candidate/skill episode with server-controlled join time |
| Placement create/start | Actual parties + candidate-wide exclusivity + queue effects + history |
| Replacement request | Coverage/owner validation + unique active request + original state + fixed SLA |
| Replacement reservation | Request offer and exclusive candidate claim agree across all skill queues |
| Replacement completion | New ACTIVE placement + original REPLACED + request COMPLETED + timestamp/SLA agree |
| Evaluation/referral | Immutable submission/finalized release and one intended referral with evidence |
| Role/account change | Audit and current authority agree; stale session cannot preserve revoked permission |

Two concurrent replacement requests cannot reserve one worker for incompatible placements. Duplicate submission has one accepted snapshot; repeated request does not reset SLA. Concurrent placement updates cannot overwrite terminal state. Technical locking/versioning is chosen during implementation, not specified as code here.

Reliable audit and notification intent must be persisted with appropriate consistency. After-commit handling/retries may use simple database-backed patterns; no broker, distributed transaction, microservices or Kubernetes required. A fixture can test matching rules but cannot substitute for real placement/notification integration acceptance.

## 8. Security, privacy and storage

Combine authentication + current account status + role + ownership/assignment + field visibility + business-state validation. Check access before returning state-sensitive errors. Restricted verification uses separate DTOs and authorized downloads; safe employer projection never includes raw NID, internal notes, emergency contacts or voice evidence.

Passwords are hashed, signing/database/provider secrets are backend environment configuration. Browser bundle configuration is public. No real sensitive identities in development/demo.

Plan a file-storage abstraction supporting configurable local filesystem storage initially and object storage later. Persist metadata/references: owner, storage key, safe display filename, size, MIME/type and lifecycle. Do not put binaries in unrelated tables or expose filesystem paths as public URLs. Validate upload size/filename/extension/MIME, authorize downloads and handle missing files safely. Storage location, limits, retention and provider are selected in CV/upload module M5.4; persistence/backup must be considered for deployment.

## 9. Integration boundaries and deployment

| Integration | MVP / future |
| --- | --- |
| Web Speech API | Optional browser speech-to-text; editable confirmed input and manual fallback |
| Verification | Manual/platform review; no government-authorized claim |
| Notifications | IN_APP required; email/SMS/voice providers deferred |
| Institutes | Operational referral updates without portal |
| Payments/AI | Deferred, no simulated production completion |

Future providers sit behind service/adapters with clear timeout/failure/retry semantics; unavailable providers must not disable core manual/in-app MVP workflows.

Deployment model: browser loads React static assets, calls Spring Boot over HTTPS, and Spring Boot accesses private MySQL/storage. Frontend/backend may deploy separately with explicit CORS and secure backend configuration. No hosting provider chosen in this module, no services deployed. M1 plans local tooling/Compose; M22 owns production configuration and smoke verification.

## 10. Compatibility and open choices

Preserve FIFO among eligible workers, dual-party confirmation, 24-hour SLA from accepted request to new ACTIVE start, and separate configured coverage. No new state/role/product workflow introduced.

Open: actual versions, ID/SQL types, migration and lock/claim implementation, exact pagination DTO, JWT/session controls, upload storage, commercial coverage and offer-expiry values. See roadmap dependency gates for M12/M15 and incremental security/testing. Architecture planning is not application initialization.
