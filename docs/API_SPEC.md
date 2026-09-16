# API Specification

## 1. Global Rules

Base prefix:

```text
/api
```

Use:
- DTOs;
- Bean Validation;
- backend role/ownership checks;
- safe error responses;
- direct typed success bodies.

Do not expose JPA entities directly.

Do not add `/api/v1` unless later explicitly requested.

## 2. Error Shape

Target:

```json
{
  "timestamp": "2026-09-13T12:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed.",
  "path": "/api/example",
  "fieldErrors": {
    "field": "Reason"
  }
}
```

Keep the machine-readable field name `error`.

## 3. Infrastructure

### GET `/api/health`

Public.

Response:

```json
{"status":"UP"}
```

Already established in M1.2.

## 4. Authentication

### POST `/api/auth/register`

Public.

Creates:
- CANDIDATE with candidateType; or
- EMPLOYER.

No evaluator/admin public signup. Request uses an allowlisted `accountType` (not an editable operational `role`):

```json
{"accountType":"CANDIDATE","candidateType":"TECH","fullName":"Demo Candidate","email":"candidate@example.com","password":"your-local-password"}
```

For TRADE use `candidateType: "TRADE"`. For EMPLOYER send `accountType`, `companyName`, `email`, `password`; omit `candidateType`. Candidate full name/company name is required for its respective account type. Passwords require 8–72 characters and at most 72 UTF-8 bytes. Email is normalized to lowercase. Unknown JSON fields (including role, score and status) are rejected.

Returns HTTP 201 with the same token/current-user shape as login. Duplicate email returns 409 `EMAIL_IN_USE`; validation returns 400. User, profile and registration audit entry are created atomically.

### POST `/api/auth/login`

Public.

Request: `{"email":"candidate@example.com","password":"your-local-password"}`.

Returns `{"token":"<JWT>","tokenType":"Bearer","expiresIn":3600,"user":{"id":1,"email":"candidate@example.com","role":"CANDIDATE","candidateType":"TECH","displayName":"Demo Candidate"}}`.

Invalid credentials return 401 `INVALID_CREDENTIALS`; non-ACTIVE accounts return 403 `ACCOUNT_INACTIVE`. Send `Authorization: Bearer <JWT>` on protected calls. Tokens expire without refresh. Password hashes and verification evidence are never returned.

### GET `/api/auth/me`

Authenticated. Returns the `user` object shown above (candidateType is null for non-candidates). Each authenticated request reloads the account’s current role and status, so suspension and role changes apply to existing tokens.

## 5. Candidate

### GET `/api/candidates/me`
### PUT `/api/candidates/me`

Candidate self-service. PUT accepts `fullName`, `phone`, `location`, `bio`, `educationSummary`, `experienceSummary`, `availability`, `primaryTradeCategory` and `portfolioUrl`. Name and availability are required; track is immutable. TECH stores education/HTTP(S) portfolio; TRADE stores primary trade. Phone accepts digits, spaces, `+`, parentheses and hyphens. Responses include skills, CV display filename, latest platform verification status and released assessment results only.

### GET `/api/skills`

Authenticated active users. Returns the active skill catalog (`id`, `name`, `category`). A small idempotent starter catalog is available at startup; test fixtures disable it with `app.skills.seed-catalog=false`.

### POST `/api/candidates/me/skills`
### DELETE `/api/candidates/me/skills/{skillId}`

Candidate self-service. POST accepts `skillId` and optional `proficiencyLevel` (self-reported). Duplicate skills return 409. Both operations return the updated own-profile DTO.

### POST `/api/candidates/me/cv`

TECH candidate. Multipart field `file`: PDF only, at most 5 MB, `.pdf` extension, `application/pdf` content type and PDF signature required. Path-containing filenames are rejected. Storage uses generated names outside public assets; successful replacement removes the old file and rollback removes the new file.

### GET `/api/candidates/me/cv`
### GET `/api/candidates/{id}/cv`

Own CV for candidates; active candidate CVs for employers. Authenticated attachment download with `no-store`; candidates cannot download another candidate's CV. Stored paths are never returned.

### GET `/api/candidates`

M4 access: employers. Operational-role search can be extended with its later workflow requirements.

Filters: `candidateType`, case-insensitive literal `location`, `skillId`, `availability`, and zero-based `page`. Returns `{content, totalElements, page, totalPages}`, 12 per page, newest candidate ID first. Only ACTIVE candidate accounts appear. No AI ranking. Cards include track, name, location, bio/experience, availability, trade, portfolio, CV availability, skills, latest verification status and released score/recommendation pairs. They exclude phone/email, identity references, stored filenames and internal notes.

Employers never receive raw verification evidence/internal evaluator notes.

## 6. Employer / Jobs

### GET `/api/employers/me`
### PUT `/api/employers/me`

Employer self-service. PUT accepts `companyName` (required), `industry`, `contactPhone`, `address`, `description`; ownership always comes from the authenticated account.

### POST `/api/jobs`
### GET `/api/jobs`
### GET `/api/jobs/{id}`
### PUT `/api/jobs/{id}`
### POST `/api/jobs/{id}/close`

Employer-only in M4; list and detail are restricted to the signed-in employer's jobs. Create/edit accepts `title`, `description`, `location`, `candidateType` (required) and optional `requiredSkillId`. Creation sets ACTIVE on the server. Closed jobs cannot be edited/reopened; close is idempotent. Cross-owner access returns 404. Responses include required skill name, status, creation time and shortlist count.

### POST `/api/jobs/{jobId}/shortlist/{candidateId}`
### DELETE `/api/jobs/{jobId}/shortlist/{candidateId}`

### GET `/api/jobs/{jobId}/shortlist`

All shortlist operations require ownership of the job. Add requires an ACTIVE job and an ACTIVE candidate account that is AVAILABLE and matches the job track/required skill. These matching checks do not imply verified readiness or placement eligibility; those belong to later workflows. Duplicate additions return 409. The job lock serializes additions against close; the unique constraint protects duplicate records. Owners can review/remove shortlist entries after closing. GET returns employer-safe candidate cards for active candidate accounts.

## 7. Assessments (M5)

Candidate-only endpoints:

| Method | Path | Contract |
| --- | --- | --- |
| GET | `/api/assessments` | Active, nonempty assessments matching the authenticated candidate’s immutable track. |
| GET | `/api/assessments/{id}` | Eligible assessment, with question IDs/prompts/options; never answer keys. |
| POST | `/api/assessments/{id}/attempts` | Start or resume the candidate’s one attempt for this assessment. |
| GET | `/api/assessment-attempts/me` | Own attempt history, including inactive assessments. |
| GET | `/api/assessment-attempts/{id}` | Own saved attempt and released result. |
| PUT | `/api/assessment-attempts/{id}/answers` | Replace saved answers: `{"answers":{"12":"A","13":"C"}}`. Question IDs must belong to the attempt; options are A–D. |
| POST | `/api/assessment-attempts/{id}/submit` | Require every question answered; atomically score and lock the attempt. Repeat submission returns the existing state. |

Attempt DTO: `id`, `assessment`, `status`, `answers`, `startedAt`, `submittedAt`, `autoScore`, `result`. Candidate `autoScore` and `result` stay null until release. A released result contains `score`, `recommendation`, `feedback`; internal notes are never included. TECH uses weighted percentage scoring rounded to two decimals. TRADE uses manual evaluator scoring (null automatic score). Both demos are untimed.

Evaluator-only endpoints:

| Method | Path | Contract |
| --- | --- | --- |
| GET | `/api/evaluator/attempts` | Submitted/evaluated work queue in submission order. |
| GET | `/api/evaluator/attempts/{id}` | Candidate name, submitted answers, auto-score, saved draft, internal notes, `released`, `editable`. |
| PUT | `/api/evaluator/attempts/{id}/evaluate` | Save draft: `score` (0–100), `recommendation` (HIRE_READY / NEEDS_TRAINING / REJECTED), required `feedback` (max 3000), optional `internalNotes` (max 3000). |
| POST | `/api/evaluator/attempts/{id}/release` | Release saved draft and transition SUBMITTED → EVALUATED. Released reviews are final. |

First draft assigns the authenticated evaluator. Other evaluators can inspect but cannot overwrite or release that draft. No client-supplied evaluator/candidate ID, auto-score, status or release flag is accepted. Candidate feedback is separate from internal notes; employer profile/search views include only released score/recommendation.

## 8. Booking (M5)

| Method | Path | Contract |
| --- | --- | --- |
| GET | `/api/appointment-slots` | Candidate: active future slots with `id`, `startTime`, `endTime`, `capacity`, `remaining`, `active`. |
| POST | `/api/bookings` | Candidate: `{ "slotId": 1, "purpose": "CONSULTATION", "notes": "Optional context" }`; purpose also supports INTERVIEW. Returns 201. |
| GET | `/api/bookings/me` | Candidate’s booking history. |
| POST | `/api/bookings/{id}/cancel` | Owner cancels an upcoming BOOKED appointment; repeat cancellation is harmless. |
| GET / POST | `/api/evaluator/appointment-slots` | Evaluator’s slots / publish `{ "startTime": "ISO instant", "endTime": "ISO instant", "capacity": 1 }`. Capacity 1–20; future start and later end required. |
| POST | `/api/evaluator/appointment-slots/{id}/close` | Owner closes a slot only when it has no BOOKED appointments. |
| GET | `/api/evaluator/bookings` | Bookings for the current evaluator’s slots, including candidate name, purpose and notes. |

Booking DTO: `id`, `slot`, `candidateName`, `purpose`, `status`, `notes`. Notes have a 2000-character limit. Candidate/employer/status IDs are not accepted. Candidate locks prevent concurrent overlaps across slots; slot locks prevent over-capacity bookings. Overlaps use half-open intervals, so back-to-back appointments are allowed. Slot instants are normalized to milliseconds before checking and persisting. Evaluator slot creation is serialized on the evaluator’s user row to reject overlapping slots. Unavailable/full/overlapping bookings return 409; past or invalid slot creation returns 400; non-owned resources return 404.

## 9. Verification

| Method | Endpoint | Contract |
| --- | --- | --- |
| POST | `/api/verifications/me` | Candidate only; `{identityReference}` (required, nonblank, max 255). Returns 201 status record. |
| GET | `/api/verifications/me` | Own history, newest submission first: `{id,status,submittedAt,reviewedAt}[]`. No evidence or internal notes. |
| GET | `/api/evaluator/verifications` | Evaluator-only pending/in-review cases, oldest first. |
| GET | `/api/evaluator/verifications/{id}` | Restricted review detail with candidate name/track/trade/location, identity reference, status, notes and timestamps. |
| POST | `/api/evaluator/verifications/{id}/review` | `{status,notes}`; status must be VERIFIED, FAILED or FLAGGED; notes required, max 3000. Final decision returns restricted detail. |
| GET | `/api/candidates/me/readiness` | Own `{eligible,checks:[{code,passed}]}` for TRADE readiness. No internal evaluation or verification data. |

Candidates cannot supply status, reviewer, timestamps or another candidate ID. Submission is serialized on the candidate; another submission is allowed only after FAILED/FLAGGED. Reviews lock the record and are final; duplicate submission/final review conflicts return 409. No actual NID provider or evidence uploads are used. Identity references and review notes are evaluator-only; employer search continues to return safe status only.

Readiness requires TRADE, ACTIVE account, latest VERIFIED record, active TRADE skill and category, relevant released HIRE_READY evaluation, AVAILABLE, no reservation and no PENDING/ACTIVE placement. `QueueEligibilityService.check(candidateId, requiredSkillId)` is the M7 selection hook; `canAdmit` additionally requires a skill and rejects existing active membership. Call under a candidate lock when implementing queue mutations. No queue mutation or queue priority is exposed by M6.

## 10. Waiting List

| Method | Endpoint | Contract |
|---|---|---|
| GET | `/api/waiting-list/me` | Candidate's queue history, status and current per-skill position. |
| POST | `/api/waiting-list/me` | Candidate admission with `{ "skillId": 1 }`; requires M6 readiness and no duplicate active membership. |
| POST | `/api/waiting-list/{id}/leave` | Owner may withdraw a QUEUED entry; reserved entries cannot be withdrawn. |
| GET | `/api/admin/waiting-list` | Admin active waiting room, ordered by joined time then ID. |

Position counts QUEUED entries in the skill, including workers whose readiness changed. Matching skips currently ineligible workers. Priority is never client-supplied. Release retains joined time if eligible; otherwise the entry exits with a reason. Activation exits all of that candidate's queue memberships.

## 11. Placements

| Method | Endpoint | Contract |
|---|---|---|
| GET | `/api/placements/me` | Candidate/employer own placements; admin may inspect all. |
| POST | `/api/placements` | Employer hiring: `{ "jobId": 1, "candidateId": 2 }`. Requires owned ACTIVE job with required active skill, shortlist membership, matching available active candidate and no reservation/PENDING/ACTIVE placement. TRADE also requires M6 readiness. |
| POST | `/api/placements/{id}/terminate` | Employer owner ends an ACTIVE placement. |
| POST | `/api/placements/{id}/complete` | Employer owner completes an ACTIVE placement. |

Hiring activates immediately. Server sets start date and a 30-day guarantee for eligible TRADE placements; TECH has no replacement coverage. Neither end action is permitted while a replacement is active. Successful replacement marks the original REPLACED and activates a new placement with its own 30-day TRADE coverage. Responses contain safe candidate/company/job/skill labels, status and coverage dates, never private evidence.

## 12. Replacement

| Method | Endpoint | Contract |
|---|---|---|
| POST | `/api/replacements` | Employer: `{ "placementId": 1, "reason": "Worker unavailable" }`, reason required, max 2,000 characters. Requires ownership, ACTIVE placement, unexpired coverage and no conflicting active request. |
| GET | `/api/replacements` | Requests visible to owning employer, original/selected candidate or admin. |
| GET | `/api/replacements/{id}` | Same visibility. |
| GET | `/api/admin/replacements` | Admin inspection. |
| POST | `/api/replacements/{id}/accept` | Employer records both-party agreement for CANDIDATE_SELECTED. |
| POST | `/api/replacements/{id}/complete` | Employer activates ACCEPTED selection atomically. |
| POST | `/api/replacements/{id}/cancel` | Employer cancels selected/accepted request and releases reservation. |
| POST | `/api/replacements/{id}/retry` | Employer retries FAILED request if original placement remains ACTIVE and no other active request exists. |

Spring singleton `ReplacementQueueManager` derives skill/employer/selection/statuses; FIFO uses joined time then ID, with eligibility rechecked under candidate locks. A stable catalog-row lock serializes replacement mutations across skill queues at MVP scale. No matching worker returns a persisted FAILED response with a useful reason. Accept/complete recheck eligibility; a changed worker is released and matching resumes, returning the actual resulting status (confirmation may be required again).

`requestedAt` and `targetCompletionAt = requestedAt + 24h` never change on retry. Completion records `actualCompletionAt` and ON_TIME when completion <= target, otherwise BREACHED. Before completion, overdue responses derive BREACHED from the server clock without a scheduler; otherwise PENDING. Generated timestamps use millisecond precision. Cancellation/empty queues do not claim successful fulfillment. Generic status/priority/coverage overrides are rejected.

Placement/replacement events synchronously notify in-app recipients and append allowlisted audit actions in the same transaction. Rollback removes both effects. Notification read/read-all endpoints remain owner-scoped.

## 13. Training

### GET `/api/training-programs`
### GET `/api/referrals/me`

Authorized evaluator/admin:

### POST `/api/referrals`

## 14. Notifications

### GET `/api/notifications/me`
### POST `/api/notifications/{id}/read`

Optional:
### POST `/api/notifications/read-all`

if easy/useful.

## 15. Admin Showcase APIs

Keep small:

### GET `/api/admin/stats`

Counts:
- users/candidates/employers;
- verified candidates;
- active jobs;
- active placements;
- replacement requests;
- on-time/breached replacements.

### GET `/api/admin/users`
### POST `/api/admin/users/{id}/status`

### GET `/api/admin/replacements`

Do not build a huge generic admin API.

## 16. Server-Controlled Fields

Never accept from normal client DTOs:
- role;
- accountStatus;
- verification status;
- score/recommendation;
- queue position/priority;
- placement status;
- selected replacement candidate;
- replacement/SLA status;
- audit metadata.

## 17. Pagination

Use simple convention where needed:

```text
?page=0&size=20
```

Do not spend major effort on generic dynamic sort/filter frameworks.

## 18. Duplicate / Conflict Behavior

Return `409` for:
- duplicate email;
- duplicate skill association;
- duplicate shortlist;
- booking conflict;
- duplicate active queue entry;
- conflicting active replacement request;
- illegal state transition.

## 19. API Quality

The showcase should have a coherent API, but exact endpoint count is not a grading objective.

Prefer fewer correct endpoints over large speculative CRUD coverage.

### M8 — Training and essential admin

- `GET /api/training-programs`: authenticated active program catalog.
- `GET /api/referrals`: candidates see their own referrals; evaluators see referrals they created; admins see all.
- `GET /api/referrals/eligible`: evaluator-owned (admin: all) released `NEEDS_TRAINING` evaluations for active candidates.
- `POST /api/referrals` with `{ evaluationId, programId }`: evaluator/admin only; checks evaluation ownership/release/recommendation, active program and compatible track; serializes duplicate prevention on the candidate. Creates an in-app notification and audit event atomically. No client-controlled referral status or candidate ID.
- `GET /api/admin/stats`: users, activeUsers, jobs, pendingVerifications, activePlacements, replacements.
- `GET /api/admin/users`, `/jobs`, `/skills`, `/verifications`: small showcase lists using explicit safe DTOs. Verification overview excludes evidence and private notes.
- `PATCH /api/admin/users/{id}/status` with `{ status }`: ACTIVE/SUSPENDED/FLAGGED/BLOCKED, audited and notified. Admin accounts are protected. Existing tokens lose access while an account is inactive; restoring ACTIVE restores access for still-valid tokens. Role changes are not exposed.
