# Business Workflows

## 1. Governing Rule

The backend validates:
- authenticated identity;
- role;
- ownership;
- account status;
- business state.

UI controls are not security.

## 2. Account Onboarding

### Candidate

```text
Register
→ ACTIVE User(role=CANDIDATE)
→ CandidateProfile(candidateType=TECH/TRADE)
→ Login
→ Complete Profile
```

### Employer

```text
Register
→ ACTIVE User(role=EMPLOYER)
→ EmployerProfile
→ Complete Company Profile
```

Evaluator/Admin are demo/seeded or created by privileged admin action.

## 3. TECH Candidate

```text
Profile
→ Skills
→ CV
→ Assessment
→ Evaluation
→ HIRE_READY / NEEDS_TRAINING / REJECTED
→ Employer discovery
→ Shortlist
→ Booking/interview
→ Placement
```

## 4. TRADE Candidate

```text
Profile
→ Trade Category
→ Optional Bangla Voice Input
→ Verification Submission
→ Reviewer Decision
→ VERIFIED
→ Assessment/Evaluation
→ HIRE_READY
→ Waiting List
→ Placement
```

Manual onboarding must work without voice.

## 5. Assessment

Stored attempt:

```text
IN_PROGRESS
→ SUBMITTED
→ EVALUATED
```

Auto-scoring may happen on submission.

Evaluator can confirm/add score, feedback and recommendation.

Candidate cannot modify a submitted attempt.

## 6. Evaluation Recommendation

- `HIRE_READY`
- `NEEDS_TRAINING`
- `REJECTED`

`NEEDS_TRAINING` may create/referral opportunity.

`REJECTED` is not an account ban.

## 7. Booking

```text
Available Slot
→ BOOKED
→ COMPLETED
```

Alternative:
- `BOOKED → CANCELLED`
- `BOOKED → NO_SHOW`

Rules:
- no past slot;
- no capacity overflow;
- no duplicate conflicting booking.

## 8. Verification

```text
PENDING
→ IN_REVIEW
→ VERIFIED
   or FAILED
   or FLAGGED
```

Candidate submits data but cannot decide outcome.

`FLAGGED` means review/risk status, not criminal guilt.

## 9. Waiting List

Automatic/approved admission requires:
- TRADE candidate;
- ACTIVE user;
- VERIFIED;
- HIRE_READY where the chosen flow requires it;
- correct skill/category;
- AVAILABLE;
- not incompatible with active placement;
- no duplicate active queue entry.

States:

```text
QUEUED
→ RESERVED
→ EXITED
```

A reservation released after failed replacement can return to QUEUED if still eligible.

## 10. Placement

```text
PENDING
→ ACTIVE
→ COMPLETED
```

or:

```text
ACTIVE
→ TERMINATED
```

If replacement succeeds, original placement may be marked `REPLACED` or `TERMINATED` according to implementation.

The service must remain internally consistent; do not maintain duplicate contradictory status fields.

## 11. Replacement

### Request

Employer can request only when:
- owns placement;
- placement is active/eligible;
- guarantee has not expired;
- no conflicting request is already active.

On request:
- status `REQUESTED`;
- `requestedAt = now`;
- `targetCompletionAt = requestedAt + 24 hours`;
- SLA `PENDING`.

### Matching

```text
REQUESTED
→ MATCHING
```

Queue manager:
1. determines skill/category;
2. queries QUEUED candidates FIFO;
3. rechecks eligibility;
4. reserves first eligible candidate atomically;
5. sets selectedCandidate;
6. changes to `CANDIDATE_SELECTED`;
7. creates notifications.

If none:
- status `FAILED`;
- failure reason records queue exhaustion.

### Acceptance

For showcase:

```text
CANDIDATE_SELECTED
→ ACCEPTED
```

The UI may represent both-party confirmation in a simplified demonstrable way.

### Completion

Create/activate replacement placement, then:

```text
ACCEPTED
→ COMPLETED
```

Set:
- `actualCompletionAt`;
- `ON_TIME` if completion <= target;
- otherwise `BREACHED`.

Never reset the SLA clock during retry.

## 12. Replacement Failure / Retry

If selected candidate becomes unavailable before completion:
- release reservation;
- continue with next eligible candidate when practical;
- preserve original requestedAt/targetCompletionAt.

A simple retry implementation is enough; no workflow engine is required.

## 13. Training Referral

```text
NEEDS_TRAINING
→ choose relevant active TrainingProgram
→ create Referral(REFERRED)
→ notify candidate
```

Further statuses:
- CONTACTED
- ENROLLED
- COMPLETED
- CANCELLED

No commission/payment automation.

## 14. Notifications

In-app only.

Trigger on high-value events:
- verification result;
- evaluation result;
- shortlist;
- booking;
- placement;
- replacement selection/completion;
- referral.

Unread/read state.

## 15. Audit

Audit important sensitive state changes:
- verification decision;
- evaluation completion;
- account admin action;
- placement/replacement;
- referral;
- role/account seed/admin operation.

Audit details must not contain secrets or raw sensitive evidence.

## 16. Account Status

- `ACTIVE` — normal operation.
- `SUSPENDED` — normal protected business operations blocked.
- `BLOCKED` — blocked.
- `FLAGGED` — operationally restricted/review state.

Exact reinstatement UI can be admin-only and simple.

## 17. Demo-Critical Failure Cases

Must handle:
- wrong employer requests replacement;
- guarantee expired;
- duplicate active replacement;
- empty queue;
- unverified candidate queue attempt;
- booking conflict;
- cross-employer job edit;
- candidate attempts to set verification/score.

These are high-value judging/security demonstrations.
