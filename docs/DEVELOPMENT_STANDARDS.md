# Development Standards

> M0.4 — documentation and architecture definition only
> Version 1.0 | 2026-09-12 | Nisa371/First-Project
> Applies to future implementation; no application, tooling, configuration, tests or dependencies are created by this module.

## 1. Purpose

Define consistent engineering conventions for the Verified Skill & Career Managed Marketplace with Voice-First Accessibility. Future modules may instruct: “Follow docs/DEVELOPMENT_STANDARDS.md.”

These rules cover repository structure, naming, layers, APIs/DTOs, validation/errors, persistence, security, frontend behavior, environments, Git, dependencies, documentation, testing and completion. They do not redesign M0.1–M0.3 or choose unresolved business policies.

## 2. Source of truth and conflict handling

| Priority | Source | Responsibility |
| --- | --- | --- |
| 1 | [MASTER_SPEC.md](../MASTER_SPEC.md) | Approved product scope, architecture and master-level decisions |
| 2 | Detailed docs under docs/ | Specific behavior and engineering contracts |
| 3 | Implemented code | Realization of approved decisions; evidence of actual current functionality |
| 4 | README/setup documentation | Accurate operational instructions for what exists |

Read [Roles and Permissions](ROLES_AND_PERMISSIONS.md) for access boundaries and [Business Workflows](BUSINESS_WORKFLOWS.md) for W01–W21, transitions, events and invariants.

This hierarchy is a consistency rule, not permission to misreport code behavior. If implementation and approved specifications disagree, identify the mismatch, preserve unrelated work, and resolve it explicitly. A new approved decision must update the affected specifications and code together. An explicit current user instruction can authorize a change; never silently infer architectural approval from a vague request.

Historical M0.1/M0.2 notes that left policy open are resolved by M0.3's master-level decisions where stated. In particular, formal employer verification is optional/future, FIFO is the MVP queue policy, and normal protected actions require ACTIVE.

## 3. General engineering principles

Prefer clear, focused, maintainable, testable code using the approved frameworks. This is an academic MVP and startup prototype; avoid premature microservices, Kubernetes, brokers, CQRS, distributed transactions, complex domain frameworks or caching infrastructure.

Keep business rules out of React presentation components, Spring controllers and repositories. Organize by feature, with modest internal layers as needed. Reuse existing abstractions before creating duplicates; do not build a generic framework for a single use case.

Retain the academic Strategy, Factory, Spring-managed Singleton and Observer requirements from MASTER_SPEC.md where appropriate. A singleton service is not a concurrency mechanism, and events must not publish success for rolled-back operations.

> Never claim a feature is complete if it is only represented by comments, placeholders, hard-coded responses, or TODOs.

Mocks/stubs are allowed only when the requested module explicitly calls for them, with accurate labels and limitations.

## 4. Repository and module organization

Current repository contains documentation only. The following are planned paths, not directories to create in M0.4.

| Area | Convention |
| --- | --- |
| Repository root | MASTER_SPEC.md and eventual concise README/setup entry points |
| docs/ | Detailed specifications, approved decisions and later API/configuration documentation |
| backend/ | Maven/Spring Boot application when initialized |
| backend/src/main/java/com/marketplace/ | Intended package root; reuse any explicitly approved established root |
| backend/src/test/java/com/marketplace/ | Tests organized by production feature responsibilities |
| frontend/src/ | React/TypeScript source when initialized |

Planned backend features: config, common, auth, user, candidate, employer, job, assessment, appointment, voice, verification, placement, queue, referral, notification, audit, dashboard.

A small feature may colocate CandidateController, CandidateService, CandidateRepository and CandidateEntity with dto/ and mapper/. Larger features may use controller/, service/, repository/, dto/, mapper/, entity/, event/ and exception/ subpackages. Choose one established layout per feature; avoid unnecessary nesting and avoid globally collecting all features' controllers/services into giant layer directories.

Frontend shared areas remain app/, pages/, components/, layouts/, services/, hooks/, types/ and router/. Feature-specific components, hooks, types and API modules belong under features/<feature>/ where useful. Preserve M0.1 names: auth, candidates, employers, assessments, appointments, voice, verification, placements, replacements, referrals, notifications, admin and dashboard. Job functionality can use features/jobs/ when introduced. Do not alternate candidate/ and candidates/ for the same frontend feature.

Existing source and configuration conventions take precedence over illustrative filenames until an explicit change is approved. Do not create placeholder modules merely to match this list.

## 5. Java and backend coding style

M1.2 confirms Java 21, Spring Boot 4.1.1 and Maven 3.9.16 via Maven Wrapper 3.3.4. Use UTF-8, LF and four-space Java indentation. Run commands from `backend/`; see [backend setup](../backend/README.md). Other runtime/version choices remain owned by their setup modules.

| Element | Naming |
| --- | --- |
| Classes/interfaces | PascalCase, e.g. CandidateService, AssessmentStrategy |
| Methods/local variables/fields | camelCase with meaningful domain names |
| Constants and enum values | UPPER_SNAKE_CASE |
| Packages | Lowercase, feature-oriented, under established package root |

Use small focused methods, early validation/clear branches, and manageable services. Avoid deeply nested logic, giant services, wildcard/unused imports, commented-out dead code, and unnecessary static mutable state.

Prefer explicit constructor dependencies for testability. Use Lombok only where it makes code clearer within the approved stack; avoid generated entity methods that traverse associations or reveal sensitive fields. Avoid blanket entity toString/equality generation that obscures persistence behavior.

Use the formatter adopted at initialization. No unrelated formatting changes. Comments explain why, business invariants, or non-obvious constraints rather than restating assignments. JavaDoc is useful for complex public contracts, not mandatory noise on every getter.

## 6. Backend layer responsibilities

| Layer | Responsibilities | Must not do |
| --- | --- | --- |
| Controller | Accept request, validate request DTO, call service, return response DTO and correct HTTP status | Repository queries, queue/business logic, ownership orchestration, transactions |
| Service/application/domain service | Ownership/assignment validation, business rules, allowed transitions, transactional orchestration, repository calls, coordination and events | Leak persistence entities to HTTP consumers or bypass security for internal calls |
| Repository | Persistence and explicit JPA queries/lookups | Business workflows, role-based business decisions or controller responses |
| DTO | Define allowed request/response contract | Mirror every entity field or become a generic arbitrary update channel |
| Mapper | Explicit entity/domain-to-DTO and permitted input transformation | Hide authorization policy or copy server-controlled fields indiscriminately |

Simple controller/method security annotations may provide role checks; meaningful ownership and workflow validation remains in services. Query scoping supports those decisions but does not replace them.

Manual mapping is acceptable initially. Do not add MapStruct or another mapping dependency without demonstrated need. A mapper shapes an already-authorized view; it is not permission to retrieve every restricted field first and expose it accidentally.

## 7. Entities, IDs and database conventions

Entities represent persistence, use stable IDs and carefully defined relationships, and are never directly serialized by REST controllers. Avoid unnecessary eager loading, accidental bidirectional JSON graphs, and unbounded relationship traversal. Use important timestamps such as id, createdAt and updatedAt where meaningful; no mandatory inheritance hierarchy when composition is simpler.

> The final ID strategy will be selected during the database design phase and then applied consistently across core entities.

Long auto-increment IDs and UUIDs remain options for the M2/database phase. Numeric documentation examples do not select Long. Do not mix strategies arbitrarily; any justified exception must be documented. External IDs or UUIDs never prove ownership.

Database tables/columns use snake_case: candidate_profiles, replacement_requests, assessment_attempts, candidate_id, created_at, verification_status. Java fields and JSON properties use camelCase. Persist business enum names as strings where practical, not ordinals; once stored/exposed, renaming requires an explicit compatibility/data-migration decision.

Use meaningful foreign keys, required-field constraints and uniqueness where feasible, alongside service validation. Important examples include normalized user identifier uniqueness, valid relationship references, no duplicate active memberships and no incompatible candidate claims. Do not assume an application pre-check alone prevents races. Database design must choose a MySQL-compatible way to enforce conditional uniqueness; no unsupported constraint syntax is selected here.

MySQL remains the main database; H2 may assist development/testing but does not prove MySQL SQL, transaction or locking behavior. Schema/migration tooling is deferred to setup/database work. Do not use unsafe production schema recreation as a convenience.

Preserve historical placements, replacement requests, assessment attempts and audit records. Jobs with history are closed/archived. Queue exit preserves history. No generic destructive delete or cascading removal of business evidence; follow each workflow's retention rules and later approved privacy policy. Audit evidence is immutable through normal application operations.

## 8. DTO conventions

Use descriptive names: CreateCandidateRequest, UpdateCandidateRequest, CandidateResponse, CreateJobRequest, UpdateJobRequest, JobResponse, CreateBookingRequest and BookingResponse. These are naming examples, not authorization to create endpoints or additional candidate profiles.

Separate input/output DTOs where permissions or data differ. Avoid CandidateDTO1, CandidateData, CommonDTO and RequestData unless a genuinely generic, narrow contract exists.

Request DTOs contain only fields the caller may submit and apply Bean Validation for syntax. A candidate profile update must not contain verificationStatus, queuePriority, assessmentScore, accountRole, authoritative owner IDs or privileged state fields. Derive ownership from authenticated identity where practical. A foreign resource ID may identify an intended job/slot, but services must authorize the relationship.

Reject attempts to set forbidden server-controlled fields; do not bind request maps directly to entities. Final unknown-field handling must be consistent, with explicit tampering coverage. Do not use reflection-based mass assignment as a shortcut.

Use consumer-specific responses where required: CandidateSelfResponse, CandidateEmployerViewResponse, CandidateEvaluatorViewResponse and restricted verification views. Employers receive only eligible, hiring-safe released information; candidates do not receive internal reviewer notes; evaluators need assignment and field-level duty; ADMIN still requires operational need.

Never return passwords/password hashes, tokens/secrets, unnecessary NID/evidence, emergency contact details or raw audio through general responses. Avoid trusting frontend omission/redaction. No DTO classes are created now.

## 9. REST API conventions

Use /api/... for the initial MVP, without requiring /api/v1. Introduce explicit API versioning deliberately only when needed for compatibility.

Resource families: /api/auth, /api/candidates, /api/employers, /api/jobs, /api/assessments, /api/bookings, /api/verification, /api/placements, /api/replacements, /api/referrals, /api/notifications and /api/admin.

Prefer nouns and consistent plural resources where applicable: POST /api/jobs and GET /api/jobs/{id}, not /api/createJob or /api/getJob. Preserve established /api/verification rather than renaming blindly. A meaningful action endpoint such as POST /api/replacements/{id}/accept is an illustrative possibility only; it must follow the approved dual-party confirmation workflow and cannot imply arbitrary status updates.

| Method | Meaning |
| --- | --- |
| GET | Retrieve without changing business state; never mark notifications read or finalize a workflow through a GET |
| POST | Create or explicitly trigger an approved business action; retries follow workflow-specific duplicate rules |
| PUT | Replace a complete client-editable representation where that contract is appropriate |
| PATCH | Partial permitted-field update with documented null/omission semantics |
| DELETE | Delete only where domain rules authorize it; use explicit archival/status workflows for retained business history |

Do not accept arbitrary verification/placement/replacement/account statuses through generic updates. Important transitions use named service operations with guards. Repeated POST requests may intentionally return an existing result where M0.3 defines idempotency; do not equate all duplicates with failure.

Return typed resource/collection success bodies directly, without a universal success/data wrapper. Paginated responses use a documented pagination DTO. Use 201 and a resource location where appropriate for newly created resources; 204 has no response body. Returning HTTP 200 with a hidden error in a success object is not permitted.

| Status | Convention |
| --- | --- |
| 200 | Successful read/update or return of an existing logical outcome |
| 201 | New resource created |
| 204 | Successful action with no body |
| 400 | Malformed request, invalid syntax or request-level validation |
| 401 | Missing/invalid authentication |
| 403 | Authenticated identity lacks permission |
| 404 | Missing resource or consistently concealed private resource |
| 409 | Authorized business-state conflict, including incompatible duplicate or invalid transition |
| 422 | Not adopted initially; requires a deliberate later contract decision |
| 500 | Unexpected server failure with safe generic client message |

Check authentication/authorization before revealing private business-state details. Consistent ownership-concealing 404 is allowed by M0.2. An expected domain outcome such as an accepted request reaching FAILED because its queue is empty is not automatically HTTP 500; a successful read still returns that request resource. Ordinary metadata/logging does not authorize business mutation on GET.

## 10. Error handling and machine-readable errors

Use one centralized error contract across validation, application exceptions and applicable security failures. Preserve the approved master field **error**, which carries the machine-readable error code. The M0.4 prompt's suggested code field is not adopted because it conflicts with the existing master example. Do not emit parallel code/error synonyms.

Illustrative response:

```json
{
  "timestamp": "2026-09-12T12:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed.",
  "path": "/api/candidates/me",
  "fieldErrors": {
    "phone": "Phone number is required."
  }
}
```

Required fields: timestamp, status, error, message and path. fieldErrors is optional, omitted when inapplicable, and maps a permitted request field to a safe message. Multiple violations for a field should yield a deterministic useful message; do not expose rejected sensitive values. The JSON status must match the HTTP status. Path excludes query strings/secrets.

Use stable UPPER_SNAKE_CASE codes as needed: VALIDATION_ERROR, RESOURCE_NOT_FOUND, AUTHENTICATION_REQUIRED, ACCESS_DENIED, DUPLICATE_RESOURCE, BUSINESS_RULE_VIOLATION, INVALID_STATE_TRANSITION, REPLACEMENT_NOT_ELIGIBLE, QUEUE_EMPTY and BOOKING_CONFLICT. Unexpected exceptions may use INTERNAL_ERROR. Do not build hundreds of unused codes.

| Condition | Typical status/error |
| --- | --- |
| Invalid request field | 400 / VALIDATION_ERROR |
| Missing authentication | 401 / AUTHENTICATION_REQUIRED |
| Role/ownership denied | 403 / ACCESS_DENIED, or policy-concealing 404 / RESOURCE_NOT_FOUND |
| Missing accessible resource | 404 / RESOURCE_NOT_FOUND |
| Incompatible duplicate | 409 / DUPLICATE_RESOURCE or specific BOOKING_CONFLICT |
| State/eligibility violation | 409 / INVALID_STATE_TRANSITION or REPLACEMENT_NOT_ELIGIBLE |
| Unexpected failure | 500 / INTERNAL_ERROR |

Actual endpoint contracts choose the smallest relevant code set and document domain outcomes separately from request errors. Clients branch on stable error values/status, not wording in message. Never return stack traces, SQL, credentials, secret configuration, or internal verification notes. Unexpected diagnostic detail belongs in restricted application logs.

## 11. Validation responsibilities

Request-level validation handles required fields, string lengths, email/syntactic format, ranges and basic structure using Bean Validation later. Frontend validation provides immediate guidance, never final authority.

Services validate ownership, assignment, current account status, job activity, coverage eligibility, candidate verification/readiness/availability, slot capacity, duplicate policy, and legal state transitions. Database constraints enforce appropriate persistence invariants under races.

Validate uploads for size, safe filename, extension and MIME type; storage/download authorization and content handling remain explicit upload-module requirements. Do not infer safety from a filename alone.

Do not repeat a complex business rule independently in multiple controllers or UI components. Reuse a domain/service rule and test its behavior. Untrusted input must not become SQL/filter expressions or server-controlled fields.

## 12. Pagination, filtering, sorting and performance

Large candidates/jobs/audit/notification/submission lists must be paginated. Adopt zero-based page requests with default page=0 and size=20; initial maximum size=100 unless an endpoint documents a smaller cap. Reject negative pages, zero/negative sizes and oversized requests consistently as validation errors.

Example request: ?page=0&size=20&sort=createdAt,desc. Endpoints declare allowed sort fields/directions and use a stable tie-breaker for deterministic results. Arbitrary SQL-style filters, property-path traversal or unlimited sizes are forbidden.

Filtering is explicit and role-aware: skill, candidateType, location, jobStatus or verificationStatus only where supported and authorized. A filter's existence never grants visibility to restricted verification evidence or flagged candidates.

Standardize the exact pagination response DTO when implementation starts; do not expose an incidental Spring Page serialization as an undocumented public contract. Document content and page/size/total metadata then. This module chooses request defaults, not a Java pagination class.

Avoid entire-table reads, obvious N+1 queries, huge payloads and unbounded nested responses. Use scoped queries/projections and measure concrete issues before adding caches or infrastructure.

## 13. Date and time rules

Persist important event timestamps consistently as absolute instants and expose ISO 8601 timestamps with UTC Z or an explicit offset. Avoid locale-formatted server timestamps and server-local timezone assumptions.

Examples: createdAt, updatedAt, requestedAt, guaranteeExpiresAt and completedAt. Document event semantics, not merely types; createdAt is not a substitute for actual employment start. Calendar-only dates remain distinct from timestamps. Exact Java time classes and database types are selected during implementation.

Preserve M0.3 rules: coverage is start-inclusive/expiry-exclusive for request acceptance; SLA target is accepted request + 24 elapsed hours; completion at target is on time; unfinished after target is breached. Actual completion means linked replacement ACTIVE, not notification or PENDING placement. Retry never resets the deadline. Test with controlled time rather than real sleeps.

Display dates in user-appropriate zones without changing underlying comparison semantics.

## 14. Transactions, business states and concurrency

Place intentional transaction boundaries in services around atomic workflows, not every method by habit. Controllers do not orchestrate transactions.

Required examples include placement plus queue changes, replacement selection/reservation, booking capacity, verification eligibility change, referral creation from finalized evaluation, role changes with audit, and replacement completion across original/new placement and request.

Preserve M0.3 invariants: no incompatible double reservation, no over-capacity slot, one accepted assessment submission, no conflicting active replacement request, and no stale approval/status bypass. A check followed by an unprotected write is insufficient for these races.

Select specific locking/versioning and database constraints in the relevant module. Do not claim singleton scope makes concurrent queue access safe. State changes go through authorized service operations; stale status updates must not overwrite newer outcomes.

Keep audit/notification intents consistent with committed domain outcomes, and retry side effects without duplicate business actions. A sensitive mutation cannot report success without secured required audit evidence. Use simple reliable persistence within the modular monolith; no broker/distributed-transaction dependency is mandated.

## 15. Application logging and auditing

| Application logs | Audit records |
| --- | --- |
| Diagnostics, lifecycle/error/integration failures, operational warnings | Durable business/security history in application data |
| Useful safe context and correlation | Actor, action, entity type/ID, timestamp, outcome and safe metadata |
| Restricted operational troubleshooting | Authorized admin investigation; immutable normal-workflow evidence |

Server log files do not replace audit records. Follow M0.3's event catalog, correlation and duplicate rules; notification/audit listeners must not recursively generate each other.

Use structured meaningful logs at suitable levels. Avoid dumping request/response bodies, tokens, passwords/hashes, JWT secret, database credentials, raw NID/audio, personal contact data or unnecessary reviewer notes. Sanitize user-provided logging fields. Error stack traces may be retained only in protected application diagnostics, never client responses, public documentation or screenshots.

## 16. React and TypeScript standards

Use TypeScript .ts/.tsx, functional React components and hooks. JavaScript-only application logic is discouraged except required tooling configuration.

Components/files use PascalCase such as CandidateProfile.tsx and EmployerDashboard.tsx. Hooks use useAuth, useCandidate and useNotifications; functions/variables use camelCase; appropriate constants use UPPER_SNAKE_CASE.

Define explicit API request/response interfaces/types. Avoid broad any; use unknown for untrusted external values and narrow safely. Compile-time types alone do not validate server/browser input at runtime. Keep types aligned with DTO field names, nullability, error structure and string enum values.

Components are focused; pages compose smaller components. Keep business truth on the server, avoid giant dashboards, duplicated form rules, raw API configuration in presentation components and hard-coded API URLs.

## 17. Frontend architecture, services and state

Use one shared Axios HTTP client, conceptually services/api.ts, with environment-based base URL and consistent error handling. Feature modules expose authApi.ts, candidateApi.ts, jobApi.ts or assessmentApi.ts as appropriate. Components call feature hooks/services instead of duplicating Axios setup.

Keep shared reusable components in components/, layouts in layouts/, and feature-specific code inside its feature. Respect existing package/file naming once initialized.

Prefer component state and limited React Context for shared concerns such as authentication. Server-state tooling can be introduced later with a documented reason. TanStack Query, React Hook Form and Zod remain optional; do not add Redux or another heavy state layer just because the roadmap is large.

Do not automatically replay every failed mutation. Retry behavior follows W01–W21 duplicate rules, especially booking, assessment submission and replacement acceptance. UI button disabling is helpful but never a server duplicate guarantee.

Token storage/refresh behavior, CORS/CSRF controls and client session handling must be decided in the authentication module; do not hard-code a storage choice in advance.

## 18. UI states, accessibility and language

Data-driven screens must handle loading, success, empty, validation and error states. Failed requests must not leave blank screens. Show safe, understandable errors and allow appropriate recovery. Preserve submitted input where safe rather than losing it on a validation failure.

Use semantic HTML, form labels, keyboard-accessible controls, visible focus, usable contrast, descriptive buttons, and meaningful field feedback. Do not rely on color, audio or icons alone to convey required actions/errors.

Trade UI prioritizes large touch targets, Bangla-friendly typography, minimal typing, understandable icons, progress indicators and simple mobile navigation. Speech requires user control, visible listening state, editable transcript and explicit confirmation. Manual entry must remain usable when recognition is unsupported, denied or fails. Voice input is not biometric authentication.

Code identifiers are English; documentation primarily clear English. Bangla is appropriate for localized end-user labels, guidance and accessibility content, not Java/TypeScript identifiers.

## 19. Configuration

Environment-specific URLs, origins, credentials and service settings belong in validated configuration, not scattered literals. Separate safe defaults from required secrets. Missing/invalid required configuration should fail clearly without printing sensitive values.

Illustrative names, to finalize during setup:

| Variable | Scope | Notes |
| --- | --- | --- |
| DB_URL | Backend | Connection target; keep credentials out of URL/logs where practical |
| DB_USERNAME | Backend | Deployment configuration |
| DB_PASSWORD | Backend secret | Never client-side or committed |
| JWT_SECRET | Backend secret | Required secure external value when JWT is implemented |
| CORS_ALLOWED_ORIGINS | Backend | Explicit deployment origins; not an authorization substitute |
| VITE_API_BASE_URL | Frontend public config | API address only; visible to users |

Document each actual variable's purpose, required/optional status, safe default if any, and local setup instructions in README or dedicated configuration docs. Do not claim .env is automatically loaded by every runtime; define loading/precedence at setup.

No .env, Docker configuration, MySQL configuration, Spring profile or Vite configuration is created in M0.4.

## 20. Secrets and environment files

Commit future .env.example with placeholders and safe explanatory defaults only. Do not commit .env, .env.local, production secret files, API tokens, private keys or real credentials. Configure appropriate ignore rules during setup; do not assume a filename is ignored merely because it contains “env.”

Anything shipped to the browser is public. VITE_-prefixed values must never contain JWT signing secrets, database passwords, provider secrets or private keys. Use environment variables/secure deployment configuration on the backend.

Secrets must not appear in frontend source, Git history, logs, test fixtures, screenshots, command output, documentation or examples. Test-only fictional dummy credentials must be unmistakably non-production and isolated; never add a production fallback secret to make tests pass.

If a real secret is accidentally exposed, report the incident without repeating the value and use an authorized rotation/remediation process. Do not silently rewrite shared Git history. No production secrets are required for this module.

## 21. Git workflow and change scope

Keep main stable. Optional feature branches may use feature/m4-auth, feature/m5-candidate-profile, fix/replacement-queue or docs/workflows. These are naming examples, not a reassignment of roadmap IDs. No complicated GitFlow, mandatory PR process or branch-protection change is introduced here.

Follow the user's authorized repository workflow; direct documentation commits to main in this session remain compatible. Never use these standards to invent a new approval gate. For shared development, a scoped branch/PR is useful when the task calls for it.

Inspect relevant files and current branch/worktree first. Preserve unrelated user changes and existing conventions. Modify only necessary files; avoid duplicate helpers/services and whole-file rewrites for small changes. Do not reformat unrelated files.

Make coherent module/submodule commits after review and relevant verification. Avoid unrelated features in one commit, unexplained enormous generated diffs, intentionally broken commits, or committed artifacts/dependencies/secrets that do not belong in source control. Do not force-push/reset shared work or bypass protected workflows without explicit authority.

List every created/modified file in the final summary. A commit is not proof tests passed.

## 22. Commit message conventions

Use concise, descriptive conventional prefixes:

| Prefix | Purpose | Example |
| --- | --- | --- |
| feat: | New implemented behavior | feat: add candidate profile endpoint |
| fix: | Correct behavior | fix: prevent duplicate replacement requests |
| docs: | Documentation | docs: define development standards |
| refactor: | Internal change without intended behavior change | refactor: extract assessment strategy factory |
| test: | Tests | test: cover replacement coverage expiry |
| chore: | Tooling/maintenance | chore: document runtime setup |
| style: | Formatting-only source change | style: format candidate service |

Optional scopes may be used consistently once established. Avoid “update,” “changes,” “done,” “final,” “stuff” or “work” as standalone messages. Explain non-obvious compatibility/decision changes in the body when needed.

## 23. Dependencies, tooling, formatting and linting

Before adding a dependency, check whether the existing stack solves the problem adequately. Explain what the dependency does, why needed, where used, and relevant compatibility. Prefer mature maintained packages and minimal additions; record approved choices and actual versions.

Do not silently replace React with Next.js, Spring Boot with Node.js, MySQL with MongoDB, Tailwind with another UI framework, or the monolith with microservices. Explicit architectural approval is required for such changes.

Once initialized, preserve consistent Maven and frontend package-manager/lockfile conventions and runtime declarations. Do not introduce competing package managers or regenerate unrelated dependency files. Do not add a package solely to complete an optional roadmap possibility.

Use the adopted Java formatter and existing Prettier/ESLint configuration if installed. At frontend setup, choose practical lint checks prioritizing unused variables, unsafe TypeScript, invalid hooks usage and likely bugs. Avoid excessive purely stylistic rules. No formatters, lint configs or packages are installed now.

## 24. Documentation and future prompts

Comments explain intent, non-obvious business rules, tricky logic or constraints. Do not maintain commented-out dead code. A TODO must state what remains, why and the relevant approved future module/milestone; do not invent a module number or use TODOs to present unfinished behavior as complete.

When an approved decision changes, implement only authorized scope, update the relevant detailed specification and MASTER_SPEC.md if a master decision changed, synchronize affected API/setup docs, and mention the decision in the final summary.

For each implemented major endpoint, document method/path, role and ownership restrictions, request/response shapes, HTTP/error cases, state guards, pagination/retry behavior where relevant and data privacy. Do not document an illustrative example as an existing endpoint.

README eventually explains overview, architecture, prerequisites, backend/frontend run commands, database setup, environments, test commands, fictional demo accounts and links to detailed docs. It is not the detailed business-rule source. Do not invent runnable commands or demo accounts before setup exists.

Future prompts should ideally specify module ID, goal, prerequisites, files/modules involved, functional/business/security requirements, allowed/forbidden edit scope, tests, acceptance criteria and expected final summary. Do not proceed to later modules simply because they appear in the roadmap.

## 25. Testing standards

Tests should verify meaningful behavior, not trivial getters/setters or copies of implementation. Backend test organization approximately mirrors candidate, assessment, queue and replacement responsibilities under the established feature packages.

| Test kind | Focus |
| --- | --- |
| Unit | Service/domain rules, assessment strategy, placement/referral/queue decisions |
| Repository | Custom queries, scoping and meaningful database constraints |
| Integration | Registration/login, evaluation, verification-to-queue, placement/replacement, referral and event consistency |
| Security | Authentication, role mismatch, ownership, current status, restricted projections, tampering |
| Frontend/acceptance | API states, form errors, permitted navigation, Bangla speech/manual fallback and accessibility |

Behavioral names such as shouldCreateReplacementRequestWhenPlacementIsEligible, shouldRejectReplacementWhenGuaranteeExpired and shouldPreventUnverifiedCandidateFromEnteringQueue are preferred over test1 or testService.

Use fictional identities and clearly isolated test values. No real NID, personal phone data, production passwords or credentials. Builders/factories may improve clarity later, but are not mandatory infrastructure.

Prioritize M0.3 failure paths: competing candidate claims, full slots, duplicate submissions/requests/enrollments/referrals, stale state updates, expired offers, queue exhaustion, account restriction during a live session and safe event retry/rollback. Use controlled clocks for coverage/SLA boundary behavior; no long real-time waits.

H2 alone cannot prove MySQL locking/constraints. Relevant persistence/concurrency behavior must eventually be checked against the target database. Mock external services only when appropriate and label them; do not count a mocked SMS as integration verification.

Run the smallest meaningful relevant checks, then required project build/lint gates. Do not broaden testing without a concrete risk or gate. Record exact commands and outcomes or explain missing tooling; never claim success for a command not run. Do not create tests in documentation-only M0.4.

## 26. Security coding standards

The backend enforces roles, current ACTIVE account status, ownership/assignment, allowed fields and business transitions. Client IDs, hidden buttons, route guards or a signed token alone do not grant ownership or bypass a changed account status.

Roles remain CANDIDATE, EMPLOYER, EVALUATOR and ADMIN. TECH/TRADE remain profile types. Public onboarding cannot assign privileged roles; multi-role membership does not permit self-verification/self-evaluation. ADMIN cannot retrieve secrets, arbitrarily reorder queues, erase audits or fabricate consent/fulfillment.

Test missing authentication, wrong role, cross-employer edits, cross-candidate reads, restricted verification DTOs and server-controlled-field tampering. A candidate sending verificationStatus=VERIFIED must not alter authoritative state; the field is not part of their editable DTO and the tampering attempt is rejected safely.

Use Spring Security and secure password hashing when implemented. JWT/database/provider secrets stay outside source. Define JWT lifecycle/storage and relevant CORS/CSRF policy in the authentication module. Validate file access, inputs and output projections server-side. Security protections must apply to list/search/export/download as well as single-resource operations.

## 27. External integration standards

MVP notification channel remains IN_APP. Government NID, production SMS/voice calls, payment and AI assessment are not implicitly available. Use interfaces/adapters or clearly marked local/demo implementations only when an authorized module needs them.

Document configured channel, provider, failure/retry semantics and actual verification performed. A local simulation is not government validation; stored notification is not SMS delivery; speech transcription is not biometric authentication. Do not make unavailable integrations prerequisites for core MVP workflows or store real sensitive identity data in demos.

## 28. Codex Implementation Rules

For every future coding module:

1. Read MASTER_SPEC.md.
2. Read relevant detailed docs under docs/.
3. Inspect existing source and configuration before editing.
4. Follow current approved architecture.
5. Build only the requested scope.
6. Avoid changes to unrelated modules.
7. Reuse existing abstractions before adding new ones.
8. Use exact established package/path naming.
9. Keep existing runnable applications runnable.
10. Run relevant tests/checks when possible.
11. Add/update tests for important new behavior.
12. Avoid unnecessary dependencies.
13. Never hard-code secrets.
14. Never expose entities directly through REST.
15. Never trust client-supplied IDs as proof of ownership.
16. Preserve M0.2 access-control rules.
17. Preserve M0.3 business and concurrency invariants.
18. Report and resolve specification conflicts explicitly.
19. List all created/modified files and material limitations after completion.
20. Stop at the requested module; do not implement later modules.

For documentation-only modules, preserving runnable state does not authorize creating a scaffold. Use focused edits, preserve unrelated work, and never imply planned behaviors already exist.

## 29. Definition of a completed implementation module

Use this checklist only for applicable requirements of the requested implementation module:

- Requested behavior genuinely implemented within scope.
- Approved architecture, package conventions and business invariants followed.
- Backend authorization/ownership and validation enforced.
- DTO inputs/consumer-specific outputs used; entities not exposed.
- Errors use the standard contract and documented HTTP behavior.
- Relevant tests added/updated and executed where feasible.
- Existing relevant tests and required build/lint checks pass.
- Documentation/API/setup information updated where needed.
- No secrets, unrelated modifications or unreviewed dependency additions.
- No placeholder, TODO, mock or hard-coded response presented as production functionality.
- Every changed file and any unverified requirement reported.
- Work stops at the requested module.

Backend commands will be established at setup; mvn test or the adopted Maven wrapper/build may apply. Frontend npm run build and configured lint/tests may apply once present. Prefer repository-established commands and package manager. If tooling is unavailable, report what could not be verified; do not mark that gate as passed.

## 30. M0.4 inspection, conflicts and deferred decisions

Inspection at commit b883e8a found MASTER_SPEC.md, docs/ROLES_AND_PERMISSIONS.md and docs/BUSINESS_WORKFLOWS.md only. Existing documentation was reviewed; no README, initialized frontend/backend, application source, runtime declarations, configuration or tests exist.

Compatibility decisions:

- Keep master error field error rather than the prompt's illustrative code field. Add optional fieldErrors without changing existing required field names.
- Keep plural frontend feature names candidates/employers from the master rather than adopting illustrative singular alternatives.
- Preserve M0.3's duplicate-action semantics: identical retries can return existing outcomes; only conflicting duplicates are rejected.
- Preserve all roles, candidate types, workflows, state transitions, FIFO policy, account restrictions and SLA semantics.

New engineering defaults: direct typed success bodies, no universal wrapper; zero-based pagination with default 20 and maximum 100; explicit filtering/sorting; feature-based layering; optional scoped Git branches and conventional commits. These do not change business workflows.

Deferred: exact Java/Spring Boot/Maven/Node versions; final database ID strategy; exact Java time/database types; exact pagination response DTO; environment variable names/loading; formatter/linter setup; token/session controls and previously open commercial policy. Record each actual choice in its implementing module.

M0.4 creates docs/DEVELOPMENT_STANDARDS.md and updates a concise master reference/version history only. No application feature code, tests, environment files, Docker/configuration, dependencies or migrations are created. M0.5 is not included.
