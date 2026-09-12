# Planned REST API Specification

> M0.5 | Version 1.0 | Contract plan only; no endpoint is implemented.
> Follow [MASTER_SPEC.md](../MASTER_SPEC.md), [roles](ROLES_AND_PERMISSIONS.md), [workflows](BUSINESS_WORKFLOWS.md), [standards](DEVELOPMENT_STANDARDS.md), [architecture](ARCHITECTURE.md), [database plan](DATABASE.md) and [roadmap](ROADMAP.md).

## 1. Global contract rules

Base /api, no /api/v1 requirement. JWT bearer authentication is planned for protected routes; token lifecycle/storage decisions are M4 work. All protected access requires current ACTIVE status plus role, ownership/assignment, safe projection and workflow guards. CLIENT IDs never prove ownership. ADMIN does not automatically inherit candidate/employer endpoints.

Return typed success bodies directly, paginated where needed. Central errors use timestamp/status/**error**/message/path with optional fieldErrors; preserve M0.4, not illustrative code naming. Baseline errors for every protected endpoint: 401 invalid/missing authentication, 403 permission/status mismatch, 404 missing or consistently concealed private resource, 400 malformed/invalid input, 409 state/concurrency conflict, 500 safe unexpected failure. Table errors emphasize domain-specific cases, not remove baseline protections.

GET never changes business state. PUT replaces only the documented client-editable representation; omission/null semantics must be finalized before coding. Explicit service actions own state transitions. API plans do not authorize generic entity binding or arbitrary PATCH state fields. Newly created resources return 201; identical logical retries can return 200 existing outcome; 204 has no body.

IDs in examples are symbolic/illustrative; final ID strategy is deferred. Payload fields are conceptual contract scope, not final exhaustive OpenAPI schemas. Each implementing module must finalize validation, nullability, DTO names and stable responses before exposure.

## 2. Planned endpoint catalog

### Authentication

| Method/path | Access/purpose boundary | Conceptual request | Success response | Major errors |
| --- | --- | --- | --- | --- |
| POST /api/auth/register | Public; CANDIDATE or EMPLOYER only | email, password, accountType; candidateType only for CANDIDATE | 201 RegistrationResponse: account ID, allowed membership, ACTIVE and initial profile | 400 validation/privileged role, 409 duplicate identifier |
| POST /api/auth/login | Public credentials; current account policy | email, password | 200 LoginResponse with bearer access token and expiry metadata | 401 invalid credentials; restricted-account response finalized in M4, no business access |
| GET /api/auth/me | Authenticated ACTIVE user | None | 200 CurrentUserResponse: own safe identity, memberships, status | 401/403; no credential/hash/secret |

### Candidate self-service and employer-safe search

| Method/path | Access/purpose boundary | Conceptual request | Success response | Major errors |
| --- | --- | --- | --- | --- |
| GET /api/candidates/me | CANDIDATE own | None | 200 CandidateSelfResponse | 404 no applicable profile |
| PUT /api/candidates/me | CANDIDATE own | Complete editable profile representation; no authoritative fields | 200 CandidateSelfResponse with derived completeness | 400 invalid/tampered fields; 409 conflicting change |
| GET /api/skills | ACTIVE roles needing catalog | Allowlisted search/page | 200 page of active skill/category summaries | 400 unsupported filter |
| POST /api/candidates/me/skills | CANDIDATE own | skillId, optional self-declared proficiency | 201 skill link; existing outcome on identical retry | 404 skill, 409 incompatible duplicate |
| DELETE /api/candidates/me/skills/{skillId} | CANDIDATE own | No body | 204 permitted removal; retain referenced history | 409 referenced-workflow conflict |
| POST /api/candidates/me/cv | CANDIDATE TECH own | Validated multipart CV file | 201 safe CV metadata/reference | 400 invalid upload; no public filesystem path |
| GET /api/candidates | EMPLOYER with complete profile; ACTIVE | page,size, supported candidateType/skill/verificationStatus/location/availability | 200 page CandidateEmployerViewResponse for eligible released candidates | 400 filter; 403 access; no raw verification evidence |
| GET /api/candidates/{candidateId} | EMPLOYER eligible hiring context | Candidate ID | 200 eligible CandidateEmployerViewResponse | 404 missing/concealed/ineligible; no own/admin/evaluator overload |

### Employer and jobs

| Method/path | Access/purpose boundary | Conceptual request | Success response | Major errors |
| --- | --- | --- | --- | --- |
| GET /api/employers/me | EMPLOYER own | None | 200 EmployerSelfResponse | 404 missing profile |
| PUT /api/employers/me | EMPLOYER own | Complete editable company representation | 200 EmployerSelfResponse | 400 input/tampering; 409 stale change |
| POST /api/jobs | EMPLOYER ACTIVE/complete profile | title, description, location, employmentType, candidateType, skillIds, saveAsDraft intent | 201 JobResponse | 400 invalid publish data |
| GET /api/jobs | ACTIVE permitted roles | page,size,status,location; owner context derived | 200 safe page; candidates see eligible/public ACTIVE jobs, employers own drafts and public jobs | 400 filter; no other employer private drafts |
| GET /api/jobs/{jobId} | Eligible viewer; employer own or public safe job | Job ID | 200 role-safe JobResponse | 404 missing/private |
| PUT /api/jobs/{jobId} | EMPLOYER owner | Complete editable job fields, not employerId or arbitrary lifecycle status | 200 JobResponse | 403/404 owner; 409 closed/archived update conflict |
| POST /api/jobs/{jobId}/publish | EMPLOYER owner | No arbitrary state field | 200 ACTIVE JobResponse after validation | 409 invalid transition; 400 incomplete job |
| POST /api/jobs/{jobId}/close | EMPLOYER owner; admin moderation through dedicated admin path later | Reason if needed | 200 CLOSED JobResponse | 409 invalid state |
| POST /api/jobs/{jobId}/archive | EMPLOYER owner | Optional safe reason | 200 ARCHIVED from DRAFT/CLOSED only | 409 ACTIVE must first close |
| POST /api/jobs/{jobId}/reopen | EMPLOYER owner | No state field | 200 revalidated ACTIVE from CLOSED | 409 archived/invalid state |
| POST /api/jobs/{jobId}/shortlist | EMPLOYER owner of ACTIVE job | candidateId | 201 membership or 200 identical existing membership | 409 candidate ineligible; 403/404 ownership |
| DELETE /api/jobs/{jobId}/shortlist/{candidateId} | EMPLOYER owner | No body | 204 membership removal, history retained | 403/404 owner |

### Assessment and evaluator

| Method/path | Access/purpose boundary | Conceptual request | Success response | Major errors |
| --- | --- | --- | --- | --- |
| GET /api/assessments | CANDIDATE eligible catalog | page,size,candidateType/skill as allowed | 200 applicable assessment summaries | 400 unsupported filters |
| GET /api/assessments/{id} | CANDIDATE eligible assessment | Assessment ID | 200 candidate-safe definition/questions without keys | 404 ineligible/missing |
| POST /api/admin/assessments | ADMIN assessment management duty | Definition/questions/rubric and release policy inputs | 201 managed assessment definition | 400 validation; 403 duty |
| POST /api/assessment-attempts | CANDIDATE own eligible COMPLETE profile | assessmentId | 201 IN_PROGRESS attempt | 409 attempt policy/ineligibility |
| GET /api/assessment-attempts/{id} | CANDIDATE owner | Attempt ID | 200 own submission/state and only released result view | 404 missing/private |
| PUT /api/assessment-attempts/{id}/answers | CANDIDATE owner; IN_PROGRESS only | Complete draft answers/submission | 200 saved draft | 409 already submitted/expired |
| POST /api/assessment-attempts/{id}/submit | CANDIDATE owner | Confirmed answers/submission snapshot | 200 accepted immutable attempt state; no fake immediate result | 409 changed retry, expired or invalid state |
| GET /api/evaluator/submissions | EVALUATOR assigned workload | page,size,allowed state filter | 200 page assigned submission summaries | 403 role |
| GET /api/evaluator/submissions/{id} | EVALUATOR assigned non-self case | Attempt ID | 200 duty-limited review projection | 404 unrelated |
| POST /api/evaluator/submissions/{id}/evaluation | EVALUATOR assigned scoring duty; SUBMITTED/UNDER_REVIEW | rubric score, feedback, separate internal notes, recommendation | 201 finalized EVALUATED result, initially UNRELEASED | 409 unsubmitted/conflict; 403 self-review |
| POST /api/evaluator/submissions/{id}/release | EVALUATOR assigned release duty | resultVersion, candidate/hiring-safe release scope | 200 RELEASED safe result | 409 stale/unfinalized; 400 unsafe scope |
| POST /api/evaluator/interviews/{bookingId}/notes | EVALUATOR assigned consultation/interview | Restricted notes and permitted outcome | 201 attributed note | 404 unrelated booking; 409 state |

### Appointments and voice

| Method/path | Access/purpose boundary | Conceptual request | Success response | Major errors |
| --- | --- | --- | --- | --- |
| GET /api/interview-slots | ACTIVE eligible candidate/employer/evaluator | Future date range,purpose,page,size | 200 available safe slot page | 400 invalid range; no unrelated booking details |
| POST /api/bookings | CANDIDATE own consultation; EMPLOYER own job interview; assigned scheduler | slotId,purpose; candidateId/jobId only for authorized employer relationship | 201 BOOKED; identical retry returns existing | 409 BOOKING_CONFLICT; 400 past slot |
| GET /api/bookings/me | CANDIDATE/EMPLOYER/EVALUATOR participant | page,size | 200 own relevant booking page | 403 unrelated role |
| POST /api/bookings/{id}/cancel | Own/assigned cancellation permission | Safe reason if required | 200 CANCELLED; idempotent repeat | 409 invalid state/time; 403/404 access |
| POST /api/voice/registrations | CANDIDATE TRADE own | confirmed transcript, language; candidate context derived | 201 restricted own metadata acknowledgment | 400 unconfirmed/input; no biometric claim |

### Verification and waiting list

| Method/path | Access/purpose boundary | Conceptual request | Success response | Major errors |
| --- | --- | --- | --- | --- |
| POST /api/verification/requests | CANDIDATE own applicable case | Permitted confirmed evidence references/fields | 201 PENDING case with safe status | 400 invalid evidence; 409 active-case duplicate |
| GET /api/verification/me | CANDIDATE own | None | 200 safe own status and policy-permitted submitted view | 404 no submitted case |
| GET /api/evaluator/verifications | EVALUATOR assigned verification duty | page,size,status | 200 assigned safe case summaries | 403 duty |
| GET /api/evaluator/verifications/{id} | EVALUATOR assigned verification duty; not self | Case ID | 200 minimum required restricted review evidence | 404 unrelated; 403 self-review |
| POST /api/evaluator/verifications/{id}/start-review | Assigned reviewer | No arbitrary status field | 200 IN_REVIEW from allowed state | 409 invalid state |
| POST /api/evaluator/verifications/{id}/review | Assigned authorized approval duty | decision APPROVE/REJECT/FLAG, findings and safe feedback | 200 reviewed case; server maps decision to VERIFIED/FAILED/FLAGGED | 409 not IN_REVIEW; 403 duty/self-approval |
| GET /api/waiting-list/me | CANDIDATE TRADE own | page,size | 200 own queue/availability summaries; no roster/priority editing | 403 wrong type |
| POST /api/candidates/me/availability | CANDIDATE own | availability AVAILABLE/UNAVAILABLE | 200 own availability; system reconciles eligibility/queue | 409 incompatible workflow change |

### Placement and replacement

| Method/path | Access/purpose boundary | Conceptual request | Success response | Major errors |
| --- | --- | --- | --- | --- |
| GET /api/placements/me | CANDIDATE or EMPLOYER actual party | page,size | 200 party-safe placement page | 403 nonparty role |
| POST /api/placements | EMPLOYER hiring owner | jobId,candidateId,plannedStartAt and permitted agreement context | 201 PENDING via validated hiring workflow | 409 ineligible/claimed/unconfirmed; candidate creation forbidden |
| POST /api/replacements | EMPLOYER original placement owner | placementId,reason,optional safe notes | 201 REQUESTED/SLA or 200 same accepted intent | 409 REPLACEMENT_NOT_ELIGIBLE/DUPLICATE_RESOURCE |
| GET /api/replacements | EMPLOYER own | page,size,status | 200 owned request page | 403 other role |
| GET /api/replacements/{id} | EMPLOYER owns request | Request ID | 200 safe request/SLA/current offer | 404 missing/private |
| GET /api/replacement-offers/me | CANDIDATE selected | page,size | 200 own opportunity-only page | 403 noncandidate |
| POST /api/replacement-offers/{offerId}/accept | Selected CANDIDATE or requesting EMPLOYER | Current offer identity; actor confirmation only | 200 own confirmation; server ACCEPTED only when both confirmed | 409 expired/stale/ineligible offer |
| POST /api/replacement-offers/{offerId}/decline | Selected CANDIDATE or requesting EMPLOYER | Safe reason | 200 recorded response; system releases/rematches | 409 consumed/stale offer |

### Referrals, notifications and administration

| Method/path | Access/purpose boundary | Conceptual request | Success response | Major errors |
| --- | --- | --- | --- | --- |
| GET /api/referrals/me | CANDIDATE own | page,size | 200 own referral page | 403 other role |
| POST /api/referrals | EVALUATOR assigned referral duty or ADMIN operation | candidateId,evaluationResultId,programId | 201 REFERRED or existing same intent | 409 missing need/duplicate conflict; 403 assignment |
| GET /api/training-programs | ACTIVE eligible roles | skill,page,size | 200 available program page | 400 filters |
| GET /api/notifications/me | Any ACTIVE role; own only | page,size,read state | 200 own IN_APP notification page | 403 restricted account; no restoration via inbox |
| POST /api/notifications/{id}/read | Recipient own | No body | 200 READ safe notification; repeat no-op | 404 private/missing |
| GET /api/admin/analytics | ADMIN operational duty | Allowlisted aggregate scope | 200 safe aggregate response | 403 role/duty |
| GET /api/admin/audit-logs | ADMIN authorized investigation | page,size,actor,action,entityType,date range | 200 restricted safe audit page | 400 filters; 403 duty |
| GET /api/admin/users | ADMIN user-management duty | page,size,allowed role/status filter | 200 safe account summaries, no credentials | 403 duty |

## 3. Deliberate endpoint choices and remaining workflow surfaces

- Job archival uses /close and /archive, not destructive DELETE /api/jobs/{jobId}. ACTIVE must close before archive. History is retained.
- Placement listing uses /api/placements/me for both actual party roles; do not also invent /api/employer/placements as an alias.
- Candidate search/detail above is employer-safe only. Evaluator review is through assigned /api/evaluator/...; admin operational views are separate /api/admin/... contracts to finalize later.
- No public/manual queue-enrollment or priority endpoint. Availability expresses candidate intent; server controls QUEUED/RESERVED/EXITED and FIFO.
- Reviewer decision actions do not directly bind status. /start-review and /review enforce W11 guards and duty. Admin equivalents must have the same safeguards, not an unrestricted update endpoint.
- Notification read-all is not adopted in the initial plan. No arbitrary candidate/employer “send notification” API.
- Privileged slots/outcomes, assessment management/corrections, verification reopen/flag, account status/role actions, institute/program management, referral operational transitions, placement agreement/start/termination, replacement retry and oversight require explicit scoped actions in their owning modules. Names/payloads are not finalized here; follow W01–W21 and M0.2.
- Placement PENDING creation requires the W14 confirmed-party workflow, including attributed candidate agreement. A generic employer claim is not proof of candidate consent. Authoritative start/completion is produced only by that workflow; no /replacements/{id} update accepts status=COMPLETED.
- Offer acceptance records one actor's confirmation. Candidate cannot approve the employer's replacement request or forge employer confirmation. End-to-end finalization requires actual replacement start and linked records.
- Logout/session revocation, renewal and recovery remain explicit M4 contract decisions. The three auth routes above alone do not claim a complete auth subsystem.

## 4. API-level access summary

Legend: ✅ own ordinary scope; ⚠️ scoped/conditional; ❌ not available for that role. All entries still require current account status and field restrictions.

| Endpoint group | Candidate | Employer | Evaluator | Admin |
| --- | --- | --- | --- | --- |
| Auth me / own notifications | ✅ | ✅ | ✅ | ✅ |
| Candidate self-profile/skills | ✅ | ❌ | ❌ | ❌ |
| Employer self-profile | ❌ | ✅ | ❌ | ❌ |
| Jobs read | ⚠️ Eligible/public | ⚠️ Own/public | ⚠️ Relevant/public | ⚠️ Operational |
| Job create/edit/shortlist | ❌ | ⚠️ Owner | ❌ | ⚠️ Dedicated moderation only |
| Employer-safe candidate search | ❌ | ⚠️ Eligible | ❌ Use assigned workload | ❌ Use operational surface |
| Candidate attempts | ⚠️ Own | ❌ | ❌ Use submissions | ❌ Use operational surface |
| Evaluation/verification review | ❌ | ❌ | ⚠️ Assigned duty, no self-review | ⚠️ Dedicated authorized operations |
| Booking | ⚠️ Own | ⚠️ Own hiring | ⚠️ Assigned/participant | ⚠️ Operational scheduling |
| Voice and verification self | ⚠️ Own applicable | ❌ | ❌ | ❌ |
| Waiting-list self | ⚠️ TRADE own | ❌ | ❌ | ❌ |
| Operational queue | ❌ | ❌ | ❌ | ⚠️ Recheck/removal, not priority edits |
| Placements me | ⚠️ Actual party | ⚠️ Actual party | ❌ | ❌ Use operations |
| Replacement requests | ❌ Offer-only access | ⚠️ Owned eligible | ❌ | ⚠️ Dedicated oversight |
| Own replacement offer response | ⚠️ Selected worker | ⚠️ Requesting employer | ❌ | ❌ No impersonation |
| Referrals | ⚠️ Own read | ❌ | ⚠️ Assigned duty | ⚠️ Operational |
| Analytics/users/audits | ❌ | ❌ | ❌ | ⚠️ Authorized duty |

Role assignment is only through trusted administrative operations. Public registration accountType is a limited CANDIDATE/EMPLOYER choice, never arbitrary roles input.

## 5. Conceptual request/response examples

Values below are fictional or nonfunctional placeholders, not live credentials, tokens, IDs or seed data. Responses omit unrelated fields for readability; protected fields remain server-controlled.

### Registration and login

POST /api/auth/register:

```json
{"email":"rahim.demo@example.invalid","password":"<user-supplied-secret>","accountType":"CANDIDATE","candidateType":"TRADE"}
```

201 response:

```json
{"userId":"<id>","roles":["CANDIDATE"],"accountStatus":"ACTIVE","candidateType":"TRADE","profileStatus":"INCOMPLETE"}
```

POST /api/auth/login:

```json
{"email":"rahim.demo@example.invalid","password":"<user-supplied-secret>"}
```

200 response:

```json
{"accessToken":"<issued-bearer-token>","tokenType":"Bearer","expiresAt":"<ISO-8601-expiry>"}
```

A bearer token delivered by login is an authentication credential, not a signing secret. Password hashes, JWT signing keys and infrastructure credentials are never returned. Treat token response/logging as sensitive; no real token is present here.

### Candidate profile

PUT /api/candidates/me, editable TRADE profile example:

```json
{"fullName":"Rahim Demo","phone":"<fictional-test-phone>","location":"Demo Area","bio":"AC technician"}
```

Response includes permitted own fields and server-derived profileStatus, not private reviewer notes. Skills are managed through skill endpoints. Existing candidateType is not freely toggled after domain records; type-change review is a separate future contract.

### Job creation

```json
{"title":"AC Technician","description":"Demo facilities role","location":"Demo Area","employmentType":"<approved-value>","candidateType":"TRADE","skillIds":["<skill-id>"],"saveAsDraft":false}
```

201 JobResponse includes id, owner-safe job fields and server-calculated ACTIVE after validation. Employer ID is derived, not supplied. Employment-type values remain to be finalized; placeholder is not a new enum.

### Assessment submission

```json
{"answers":[{"questionId":"<question-id>","response":"Demonstration answer"}]}
```

200 attempt response:

```json
{"id":"<attempt-id>","status":"SUBMITTED","submittedAt":"2026-09-12T10:00:00Z"}
```

Later review may change state; no claim of immediate HIRE_READY or released score. Repeated changed answers after submission are rejected.

### Verification review

```json
{"decision":"APPROVE","findings":"Fictional manual review completed.","candidateFeedback":"Platform review completed."}
```

200 restricted reviewer response may include case ID/status VERIFIED/reviewedAt, while candidate/employer routes use safe projections. Requires IN_REVIEW, complete required checks and authorized non-self reviewer. APPROVE is an action input, not a new verification status.

### Replacement request

```json
{"placementId":"<owned-placement-id>","reason":"WORKER_UNAVAILABLE","notes":"Demo worker became unavailable."}
```

WORKER_UNAVAILABLE is illustrative reason text/code; final allowed reasons must follow coverage policy.

```json
{"id":"<request-id>","status":"REQUESTED","requestedAt":"2026-09-12T09:00:00Z","targetCompletionAt":"2026-09-13T09:00:00Z","actualCompletionAt":null,"slaStatus":"PENDING"}
```

Selection/offer is separate, never submitted by employer. Retry does not change these timestamps.

### Notification

GET /api/notifications/me item:

```json
{"id":"<notification-id>","channel":"IN_APP","eventType":"REPLACEMENT_REQUESTED","title":"Replacement requested","message":"Your request was received.","readAt":null,"createdAt":"2026-09-12T09:00:00Z"}
```

readAt null represents UNREAD; authenticated recipient marks read. No raw identity data or secret embedded in message.

## 6. Server-controlled fields and state guards

Clients cannot freely assign roles, accountStatus, verificationStatus, assessmentScore, evaluationRecommendation, queuePriority, placementStatus, replacementStatus, slaStatus, audit fields, createdAt or updatedAt. Ownership identifiers, reviewer attribution, selected candidate, request/SLA timing and authoritative status are resolved by server workflow.

A narrowly authorized evaluator supplies a score/recommendation through evaluation DTO; a trusted admin supplies a permitted role/status action through a guarded operation. That does not make these fields editable in generic profile/CRUD DTOs. Public accountType selects only the allowed onboarding role. Rejected tampering must never mutate the authoritative state.

No generic employer PUT /api/replacements/{id} body can set COMPLETED. Queue exhaustion is an explicit FAILED domain outcome; reads of that request still return 200, not an unexplained server error.

## 7. Pagination, filters and duplicate behavior

M0.4 request convention is page=0, size=20, maximum 100 (or documented lower endpoint cap), with allowlisted sort=createdAt,desc and stable tie-breaker. Reject invalid/oversized parameters. Exact pagination response DTO is finalized at implementation; typed page object is required, not uncontrolled Spring serialization.

Examples: /api/jobs?page=0&size=20, /api/candidates?page=0&size=20, /api/notifications/me?page=0&size=20, /api/admin/audit-logs?page=0&size=50.

Candidate filters: candidateType, skill, verificationStatus, location, availability only within eligible employer scope. Job filters: status/location; private draft status never reveals other owners. Audit filters: actor/action/entityType/date range. No arbitrary SQL/property expressions.

Duplicate protection covers assessment submit, shortlist, booking, automatic queue admission, replacement request and referrals. Same logical retry returns existing permitted outcome; conflicting duplicate returns 409. Exact request-id/idempotency transport is deferred, but server guarantees are mandatory. Selection, expiry and acceptance also check current offer identity; stale requests cannot release/consume another claim.

## 8. Error examples

All examples retain error (not code), timestamp and path; no SQL, stack traces or secrets.

```json
{"timestamp":"2026-09-12T10:00:00Z","status":400,"error":"VALIDATION_ERROR","message":"Request validation failed.","path":"/api/candidates/me","fieldErrors":{"phone":"Phone number is required."}}
```

```json
{"timestamp":"2026-09-12T10:00:00Z","status":401,"error":"AUTHENTICATION_REQUIRED","message":"Authentication is required.","path":"/api/auth/me"}
```

```json
{"timestamp":"2026-09-12T10:00:00Z","status":403,"error":"ACCESS_DENIED","message":"You do not have permission for this action.","path":"/api/admin/users"}
```

```json
{"timestamp":"2026-09-12T10:00:00Z","status":404,"error":"RESOURCE_NOT_FOUND","message":"Resource not found.","path":"/api/jobs/example-id"}
```

```json
{"timestamp":"2026-09-12T10:00:00Z","status":409,"error":"DUPLICATE_RESOURCE","message":"An active replacement request already exists for this placement.","path":"/api/replacements"}
```

Use established DUPLICATE_RESOURCE rather than inventing REPLACEMENT_ALREADY_ACTIVE prematurely. 422 is not initially adopted. Check authorization before revealing conflict details.

## 9. Implementation handoff

This plan fixes resource groups, safety boundaries and conceptual contract shapes, not endpoint implementation. M4 onward must refine exact DTOs/validation/status codes within these conventions and add synchronized examples/tests. Any additional privileged action must be justified by existing roles/workflows and documented before exposure.

No routes, controllers, security config, tokens, test data or dependencies are created in M0.5.
