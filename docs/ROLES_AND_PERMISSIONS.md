# Roles and Permissions

> M0.2 — authorization specification only.
> Version 1.0 | 2026-09-12
> Authority: [MASTER_SPEC.md](../MASTER_SPEC.md).
> No authentication, authorization, database, API, or frontend implementation is included.

## 1. Purpose

Define the roles, candidate types, resource ownership, data visibility, and conditional permissions for the Verified Skill & Career Managed Marketplace with Voice-First Accessibility. This specification refines M0.1 without changing its stack, workflows, or MVP boundaries. Future security implementation and tests must enforce these decisions.

Canonical roles: **CANDIDATE, EMPLOYER, EVALUATOR, ADMIN**.
Canonical candidate types: **TECH, TRADE**.

TECH and TRADE are domain classifications, not authentication roles. Both authenticate as CANDIDATE. Do not introduce TECH_CANDIDATE or TRADE_CANDIDATE roles.

## 2. Authorization principles

> Every role receives only the minimum access necessary to perform its responsibilities.

- The backend is the final authority. Frontend hiding of buttons and route guards only improve UX.
- Deny access unless the action is explicitly authorized; an unspecified permission is not an implicit grant.
- Validate authenticated identity, effective role, account status, resource ownership/assignment, data visibility, and business rules together.
- Client-supplied IDs identify requested resources; they never prove ownership.
- Candidate endpoints derive candidate identity from the authenticated account where practical. Employer operations resolve the employer from the authenticated principal and validate ownership of jobs, placements, and requests.
- Evaluators see only assigned/relevant workflows and the fields needed for their duties.
- Employers receive employer-safe candidate projections; sensitive evidence requires restricted DTOs and dedicated authorization.
- Only trusted administrative operations assign roles. Public onboarding cannot grant EVALUATOR or ADMIN.
- Admin operations remain validated and audited; an admin role does not erase business rules.
- Password/security-secret data is never returned through application APIs or dashboards.
- Queue ordering is controlled by backend business logic, never ordinary candidate/employer input.
- Permissions must apply consistently to individual reads, search/list results, downloads, exports, bulk actions, and notifications.
- A role grant is not authority to impersonate another account or silently bypass an approval requirement.

Example: an authenticated candidate calling an admin-only API receives 403 even when the UI never shows an Admin button.

## 3. User / role / candidate-type model

| Concept | Relationship | Meaning |
| --- | --- | --- |
| User | Common authentication identity | Account identity and account status, separate from domain-specific profile data |
| Role membership | User → user_roles → roles | Architecture may support multiple roles; normal MVP onboarding grants one operational role |
| Candidate profile | User with CANDIDATE membership → candidate profile | Candidate-owned domain information and one candidateType: TECH or TRADE |
| Employer profile | User with EMPLOYER membership → employer profile | Organization/company-specific information |
| Evaluator authority | EVALUATOR membership plus assignment/duty | No implied access to every candidate |
| Administrative authority | ADMIN membership plus authorized operation and need | No implied access to secrets or direct database edits |

A candidate profile applies when the user's memberships include CANDIDATE, not only when CANDIDATE is their sole role. Candidate type is not stored as an authentication role. Account status, candidate verification, assessment recommendation, and candidate type are independent concepts.

Role removal/type changes must preserve historical domain records; the exact schema is deferred.

## 4. CANDIDATE

### Purpose, ownership, and relationships

A person seeking employment, assessment, training, or managed placement. Owns their account-editable fields and candidate profile, self-declared skills, CV/portfolio submissions, assessment submissions, bookings, and personal notifications. Being the subject of a result, verification, queue entry, placement, or referral does not grant control of its authoritative fields.

Candidates provide information to assigned evaluators, appear in eligible employer searches through safe projections, and participate in platform-managed hiring/training workflows. Administrators provide operational review/support.

### 4.1 Shared permissions

Both types may register, login/logout, view/edit permitted own account/profile fields, browse available skills/categories, and add/update/remove self-declared skills. Skill edits must not directly alter verified competency or invalidate active workflow references without review.

They may view own assessment history and released evaluation results; create/cancel eligible own bookings; view own bookings, application/placement status, notifications, eligible training referrals and referral status; mark own notifications as read; and view own verification status where applicable.

Allowed reads exclude unreleased results, internal evaluator/risk notes, other candidates' private information, other employers' private records, audit logs, answer keys, and system secrets. Own submitted verification data may be presented back through a policy-limited subject view, never as unrestricted reviewer-record access.

Candidates must not edit others' profiles, create employer/evaluator accounts, access admin analytics, modify scores/feedback/verification outcomes, approve themselves, set VERIFIED, create authoritative placements, approve replacements, change queue priority, or assign roles.

Bookings require eligibility, an available slot, correct participants, and cancellation/state rules. Results require release. Referrals must concern the candidate. Accepting/declining an opportunity, if implemented, records a response to an authorized offer; it is not permission to write placement status directly.

### 4.2 TECH candidate

Typical users: CSE students, developers, engineers, graduates, and professionals.

Relevant information/features: professional profile, education, experience, CV, portfolio, technical skills, online assessments, interviews, employer shortlisting, and corporate applications/placement.

May upload/manage own CV under upload controls, maintain portfolio details, take applicable technical assessments, view released results, participate in interviews/consultations, be shortlisted, and receive placement notifications.

Trade waiting-list enrollment, voice-first onboarding, trade-specific verification, and automatic replacement-pool enrollment do not apply by default. Any expansion requires an explicit business decision.

### 4.3 TRADE candidate

Typical users: drivers, electricians, technicians, mechanics, delivery riders, plumbers, and other skilled/semi-skilled field workers.

Relevant features: simplified Bangla-first onboarding, trade/service category, voice-assisted or manual data entry, consultations, verification, evaluation, waiting-list eligibility, managed placement, replacement pool, and training referrals.

May choose a trade category, submit own information and required verification data, book required consultations, view own verification and limited queue/availability status, receive placement/training notifications, and respond to relevant opportunities if implemented.

Voice input is optional speech-to-text with permanent manual fallback, not authentication or verification approval. TRADE users must not self-approve, insert themselves at the head of a queue, manipulate priority, or access employer replacement controls. Queue visibility shows their own status, not other workers' identities, private data, or arbitrary position-editing controls.

## 5. EMPLOYER

### Purpose and ownership

A hiring business/organization, such as a technology company, logistics business, retailer, restaurant, or service organization. Owns its employer/company profile, jobs/workforce requirements, shortlists, and associated placement relationships and replacement requests.

An account may register and complete its profile before any required approval. Candidate-discovery/hiring permissions require applicable employer approval; the exact approval policy remains open under M0.1. Employers cannot approve themselves.

### Allowed capabilities and viewable data

- View/edit own company profile; create, view, edit, close/archive own jobs and workforce requirements.
- Search eligible candidates and view employer-safe profiles, relevant skills, experience, portfolio, released evaluation summaries, verification status, and availability.
- Add/remove candidates in own shortlists and request eligible interviews.
- Initiate hiring through the validated placement workflow; view own placements and relevant hiring history.
- Submit eligible replacement requests for owned placements; view own request status and the safe profile of the candidate offered to that employer.
- View and mark own employer notifications as read.

CV/contact disclosure is conditional on the later hiring/privacy policy; discovery alone is not unrestricted download/contact permission.

### Prohibitions and relationships

Cannot edit candidates, manipulate scores, mark workers VERIFIED, access raw NID/identity evidence, unrelated employers' private data/jobs, internal evaluator notes unless explicitly released, audit logs, platform roles, or queue priority.

Employers interact with candidates via shortlisting/interviews/hiring; rely on evaluator-released results; and use admin-supported placement/replacement workflows. Ownership of a placement does not permit arbitrary status rewriting or choosing an ineligible replacement outside the queue workflow.

## 6. EVALUATOR

### Purpose, ownership, and responsibilities

An authorized expert/personnel member responsible for candidate readiness and applicable verification review. Owns editable personal account fields and own scheduling data; authors assigned assessment feedback, interview notes, and evaluation records within platform workflows. Authorship does not confer unrestricted deletion or control of official history.

### Allowed capabilities and conditional access

May view assigned candidates and relevant profile/submission information, review assessment attempts, score manually evaluated assessments, provide feedback/interview notes, conduct or record consultations, record results, and recommend HIRE_READY, NEEDS_TRAINING, or REJECTED.

May view evaluator-specific bookings/notifications, manage own eligible slots, and review assigned trade verification submissions. Verification approval/rejection requires an explicitly authorized verification duty and the defined workflow; EVALUATOR alone is insufficient. Referrals may be created for assigned candidates where evaluation establishes a training need.

Assessment definition/key access is restricted to authorized assessment duties. Default definition management is ADMIN; evaluator authoring requires explicit delegated scope. Released scores can be corrected only through a tracked correction workflow, not silent overwriting.

### Data limits, prohibitions, and relationships

Sensitive information must be limited to what evaluation/verification duties require. No unrelated candidate evidence, employer private data, financial information, secrets, or global audit access.

Evaluators cannot assign roles, create arbitrary admins, edit employer jobs, change platform-wide settings, manipulate queue order, fabricate placements, bypass verification, alter audits, or permanently delete audit history. They assess candidates, supply released hiring summaries to employers, and operate under admin assignment/oversight.

## 7. ADMIN

### Purpose and operational capabilities

Provides platform governance and operational management of users, candidates, employers, evaluators, account statuses, skills/trade categories, institutes/programs, assessment definitions, verification workflows, partners, waiting lists, placements, replacements, notifications, analytics, and audit access.

May activate/suspend/block eligible accounts, manage authorized metadata/reference data, review verification cases, manage partners/evaluators, investigate incidents, inspect audit events, oversee replacement failures, and manage training information. Role assignment is a privileged, audited administrative operation with safeguards defined in the later identity module.

Admins do not personally own all platform resources. They have operational stewardship and access only for an authorized duty; account ownership remains with the user.

> ADMIN access does not mean unrestricted direct database manipulation.

Operations must use defined services/workflows with validation, transition rules, and auditing. Queue oversight may remove an ineligible entry or trigger a legitimate re-evaluation/retry; it does not grant arbitrary priority editing or permission to mark an unfulfilled replacement successful.

### Data limits, prohibitions, and relationships

May view cross-user operational records and restricted verification data only where necessary for assigned operational duties. Cannot retrieve plaintext passwords/password hashes through application UI/APIs, access JWT/environment secrets through dashboards, erase audits to hide activity, bypass logging, or expose identity data without need.

Admin oversight does not confer employer/candidate identity or unrestricted evaluator scoring authority. Scoring/evaluation edits require an authorized correction/review workflow and a recorded reason. Administrators support all roles while preserving each resource's ownership and history.

## 8. Resource ownership

| Resource | Owner/subject | Other authorized access and limits |
| --- | --- | --- |
| User account | Authenticated user | ADMIN manages allowed metadata/status via audited workflows; no role/secret fields in self-edit payloads |
| Candidate profile | Candidate | Assigned EVALUATOR reads relevant fields; EMPLOYER reads safe projection of eligible candidates; ADMIN operational access |
| Employer profile | Employer | ADMIN operational management; candidates may read public/company-safe information; other employers do not gain private access |
| Job/workforce requirement | Creating employer | Candidates read eligible/public jobs; ADMIN oversight; other employers cannot modify |
| Shortlist/application | Employer owns shortlist; candidate is application subject | Candidate sees own permitted status, not employer's full shortlist/internal notes; ADMIN oversight |
| Assessment definition | Platform; ADMIN management | Explicitly authorized evaluator may author/review; candidate receives eligible questions, never internal keys; employer sees released outcomes only |
| Assessment attempt | Candidate subject/submission | Assigned evaluator reviews; ADMIN operational access; employer gets released summary only |
| Evaluation result | Platform record about candidate; evaluator authors | Candidate sees released own result; employer sees hiring-safe released summary; internal notes remain restricted |
| Booking/interview slot | Slot author; booking participants | Candidate/employer/evaluator access their relevant bookings; ADMIN scheduling oversight; unrelated details restricted |
| Verification record | Candidate subject | Candidate views status and policy-permitted own submissions; assigned authorized evaluator/admin reviews/updates; employer sees safe status only |
| Waiting-list entry | TRADE candidate subject | Backend queue logic controls ordering/eligibility; ADMIN oversight; candidate limited own status; no employer roster/priority access |
| Placement | Candidate + employer parties | Platform placement workflow creates/changes authoritative record; ADMIN oversight; evaluator access only if a defined case requires it |
| Replacement request | Employer of eligible original placement | System processes; ADMIN oversight; affected/selected candidate sees only relevant opportunity/status |
| Referral | Candidate subject | Evaluator/admin/system workflow creates; candidate sees own; institute portal access deferred until explicitly implemented |
| Notification | Addressed recipient | Recipient reads/marks read; system creates; ADMIN limited delivery/operational management, not unrestricted private inbox impersonation |
| Audit log | System-generated evidence | Authorized ADMIN reads; immutable through normal application workflows |

## 9. Permission matrix

Legend: ✅ permitted within stated ordinary scope; ⚠️ conditional/limited as stated; ❌ not permitted by this role. All grants require applicable account status, ownership, field restrictions, and workflow rules. Entries describe each role independently; ADMIN is not automatically a CANDIDATE or EMPLOYER.

“Own” means derived from authenticated identity and validated relationships. “Operational” means an authorized duty with need, validation, and audit. System processing is not a fifth human role. When a person may trigger a workflow, that does not grant direct access to its internal state fields.

### Account and profiles

| Resource / Action | Candidate | Employer | Evaluator | Admin |
| --- | --- | --- | --- | --- |
| View own account | ✅ Safe fields | ✅ Safe fields | ✅ Safe fields | ✅ Safe fields |
| Edit own account | ⚠️ Editable fields | ⚠️ Editable fields | ⚠️ Editable fields | ⚠️ Editable fields |
| Manage another user's account status | ❌ | ❌ | ❌ | ⚠️ Authorized status workflow |
| Assign roles | ❌ | ❌ | ❌ | ⚠️ Privileged audited operation |
| View own candidate profile | ✅ | ❌ | ❌ | ❌ Role alone has no candidate profile |
| Edit own candidate profile | ⚠️ Self-edit fields | ❌ | ❌ | ❌ Use operational access below |
| View another candidate | ❌ Private profile | ⚠️ Eligible safe projection | ⚠️ Assigned/relevant | ⚠️ Operational need |
| Edit another candidate profile | ❌ | ❌ | ❌ | ⚠️ Authorized support correction |
| View sensitive verification information | ⚠️ Own submission view only | ❌ | ⚠️ Assigned verification duty | ⚠️ Operational need |
| Modify candidate verification status | ❌ | ❌ | ⚠️ Authorized verification workflow | ⚠️ Verification workflow |
| View employer profile | ⚠️ Company-safe fields | ⚠️ Own full/public others | ⚠️ Relevant company-safe fields | ⚠️ Operational need |
| Edit own employer profile | ❌ | ⚠️ Editable fields | ❌ | ❌ Role alone has no employer profile |
| Edit another employer profile | ❌ | ❌ | ❌ | ⚠️ Audited operational correction |

### Jobs and hiring discovery

| Resource / Action | Candidate | Employer | Evaluator | Admin |
| --- | --- | --- | --- | --- |
| View jobs | ⚠️ Eligible/public | ⚠️ Own/private; public others | ⚠️ Relevant/public | ⚠️ Operational |
| Create job | ❌ | ⚠️ Own employer; approval rules | ❌ | ❌ Employer-originated workflow |
| Edit own job | ❌ | ⚠️ Ownership and editable state | ❌ | ❌ No employer identity implied |
| Edit another employer's job | ❌ | ❌ | ❌ | ⚠️ Operational moderation/correction |
| Delete/archive own job | ❌ | ⚠️ Close/archive; preserve history | ❌ | ❌ No employer identity implied |
| Search eligible candidates | ❌ | ⚠️ Safe projection; employer eligibility | ⚠️ Assigned workload only | ⚠️ Operational |
| Add/remove shortlist entry | ❌ | ⚠️ Own shortlist; eligible candidate | ❌ | ⚠️ Audited operational correction |
| View application/shortlisting status | ⚠️ Own released status | ⚠️ Own hiring records | ⚠️ Assigned evaluation context | ⚠️ Operational |

Hard deletion of jobs with dependent history is not authorized by close/archive permission. Any later deletion policy must preserve required records.

### Assessments

| Resource / Action | Candidate | Employer | Evaluator | Admin |
| --- | --- | --- | --- | --- |
| Take assessment | ⚠️ Own applicable eligible attempt | ❌ | ❌ | ❌ Role alone not an examinee |
| Create assessment | ❌ | ❌ | ⚠️ Explicit authoring delegation | ⚠️ Definition management |
| Review assessment attempt | ⚠️ Own permitted submission/history | ❌ Raw attempt | ⚠️ Assigned attempt | ⚠️ Operational |
| Modify candidate score | ❌ | ❌ | ⚠️ Assigned scoring/correction workflow | ⚠️ Authorized correction review only |
| View released candidate result | ⚠️ Own | ⚠️ Relevant hiring-safe summary | ⚠️ Assigned/relevant | ⚠️ Operational |
| View assessment answer keys | ❌ | ❌ | ⚠️ Authorized assessment duty | ⚠️ Assessment management duty |

### Appointments

| Resource / Action | Candidate | Employer | Evaluator | Admin |
| --- | --- | --- | --- | --- |
| Create booking | ⚠️ Own eligible consultation/interview | ⚠️ Own hiring request; eligible participants | ⚠️ Assigned consultation/scheduling | ⚠️ Operational scheduling |
| Cancel booking | ⚠️ Own; cancellation rules | ⚠️ Own; cancellation rules | ⚠️ Assigned/own; rules | ⚠️ Operational; rules |
| View own bookings | ✅ Own details | ✅ Own details | ✅ Own details | ⚠️ If a participant |
| Manage evaluator slots | ❌ | ❌ | ⚠️ Own eligible slots | ⚠️ Operational scheduling |
| View unrelated bookings | ❌ | ❌ | ❌ | ⚠️ Operational need only |

### Verification

| Resource / Action | Candidate | Employer | Evaluator | Admin |
| --- | --- | --- | --- | --- |
| Submit verification data | ⚠️ Own applicable case | ❌ Candidate verification | ❌ No candidate impersonation | ⚠️ Assisted intake with provenance |
| Review verification | ❌ Reviewer authority | ❌ | ⚠️ Assigned verification duty | ⚠️ Authorized case review |
| Approve/reject verification | ❌ | ❌ | ⚠️ Assigned approval duty; workflow | ⚠️ Workflow; no self-approval |
| View employer-safe verification status | ⚠️ Own | ⚠️ Eligible candidate | ⚠️ Assigned case | ⚠️ Operational |
| View raw sensitive verification data | ❌ Reviewer evidence endpoint | ❌ | ⚠️ Minimum assigned evidence | ⚠️ Minimum operational evidence |

The candidate's limited view of their own submitted fields/status is separate from raw reviewer evidence, internal notes, or bulk document access. Exact self-submission fields and redaction must be defined before implementation. Employer verification, if introduced, is a separate case type and never self-approved.

### Waiting lists

| Resource / Action | Candidate | Employer | Evaluator | Admin |
| --- | --- | --- | --- | --- |
| Enter queue automatically when eligible | ⚠️ TRADE subject; system action | ❌ | ❌ | ⚠️ Trigger eligibility recheck only |
| View own queue/availability status | ⚠️ TRADE; limited own status | ❌ | ❌ | ❌ No candidate identity implied |
| View operational queue | ❌ | ❌ | ❌ | ⚠️ Operational need |
| Modify queue order | ❌ | ❌ | ❌ | ❌ Arbitrary priority edits |
| Remove candidate for operational reason | ❌ Direct queue mutation | ❌ | ❌ | ⚠️ Reasoned workflow; system recalculates |

A candidate may update self-declared availability through a validated profile workflow; only backend queue logic applies the resulting eligibility change. Admin removal/recheck cannot be used to circumvent ordering. M0.1's proposed FIFO rule remains subject to later finalization.

### Placements

| Resource / Action | Candidate | Employer | Evaluator | Admin |
| --- | --- | --- | --- | --- |
| View own placement | ⚠️ Party-safe details | ⚠️ Owned placement | ❌ | ❌ Use operational access below |
| Create placement | ❌ Authoritative record | ⚠️ Initiate own hiring workflow | ❌ | ⚠️ Operational workflow for actual parties |
| Modify placement status | ❌ Direct status edit | ⚠️ Permitted own workflow transition | ❌ | ⚠️ Validated operational transition |
| View unrelated placement | ❌ | ❌ | ❌ | ⚠️ Operational need |

Candidates may respond to an offer if implemented; the system, not an arbitrary candidate-supplied status, produces any resulting placement transition.

### Replacements

| Resource / Action | Candidate | Employer | Evaluator | Admin |
| --- | --- | --- | --- | --- |
| Request replacement | ❌ | ⚠️ Owned eligible placement | ❌ | ❌ Employer originates request |
| Process replacement | ❌ | ❌ Internal processing | ❌ | ⚠️ Trigger/oversee system workflow |
| View own replacement request | ⚠️ Affected opportunity/status only | ⚠️ Own request | ❌ | ⚠️ Operational access |
| Override failed replacement operationally | ❌ | ❌ | ❌ | ⚠️ Audited retry/resolution, not eligibility bypass |
| Accept/decline offered opportunity | ⚠️ Selected candidate if implemented | ⚠️ Employer's offered replacement | ❌ | ❌ No party impersonation |

### Referrals and notifications

| Resource / Action | Candidate | Employer | Evaluator | Admin |
| --- | --- | --- | --- | --- |
| View own referral | ⚠️ Own candidate referral | ❌ | ❌ Use assigned access below | ❌ Use operational access below |
| View referral for another candidate | ❌ | ❌ | ⚠️ Assigned relevant case | ⚠️ Operational |
| Create training referral | ❌ Official record | ❌ | ⚠️ Assigned candidate; training need | ⚠️ Validated referral workflow |
| Update referral operational status | ❌ | ❌ | ⚠️ Assigned referral duty and evidence | ⚠️ Validated operational transition |
| View own notifications | ✅ | ✅ | ✅ | ✅ |
| Mark own notification as read | ✅ | ✅ | ✅ | ✅ |
| Send platform/system notifications | ❌ | ❌ | ❌ | ⚠️ Authorized operational messages |

Normal authorized business actions by any role may produce system-generated notifications; that is not arbitrary message-sending permission. No institute account/portal is introduced.

### Platform administration

| Resource / Action | Candidate | Employer | Evaluator | Admin |
| --- | --- | --- | --- | --- |
| View analytics | ❌ Platform analytics | ❌ Platform analytics | ❌ Platform analytics | ⚠️ Authorized aggregates |
| Manage skills/categories | ❌ Global catalog | ❌ | ❌ | ⚠️ Reference-data workflow |
| Manage training institutes/programs | ❌ | ❌ | ❌ | ⚠️ Partner-management workflow |
| View audit logs | ❌ | ❌ | ❌ | ⚠️ Authorized investigation |
| Delete audit logs | ❌ | ❌ | ❌ | ❌ Normal application operation |
| Manage system configuration | ❌ | ❌ | ❌ | ⚠️ Approved non-secret settings |

Own role dashboards/status summaries are distinct from platform-wide analytics. Secrets and infrastructure configuration remain outside ordinary application UI/APIs, including ADMIN.

## 10. Conditional access rules

### Employer candidate visibility

Check the employer's current account/approval eligibility and candidate discovery eligibility, then return an employer-safe DTO. No raw entity response. Professional fields are not automatically public merely because they are low sensitivity. Search eligibility, released outcomes, and any CV/contact disclosure must follow the hiring/privacy policy. Private notes, answer keys, internal risk flags, raw NID data, emergency contacts, and voice evidence are excluded.

### Verification and evaluation

Only assigned/relevant authorized evaluators or operational admins may update case outcomes through required review transitions. Candidates submit input, never approval fields. Verification duty, score-authoring scope, result release, and correction permissions are distinct. An account must not approve/evaluate its own candidate record, including when it has additional roles; refer the case to another authorized reviewer.

Candidate access to results requires release; employers receive only hiring-released summaries. Unreleased/internal evaluator notes remain restricted. Altering a profile/skill after review must not silently turn self-declarations into verified claims.

### Booking

Validate eligible participants, relationship to the interview/consultation, slot availability, and cancellation constraints. Knowledge of a slot/booking ID does not authorize viewing or editing unrelated participant data.

### Waiting-list eligibility

A candidate enters/becomes selectable only when candidateType is TRADE, account is ACTIVE, required verification is VERIFIED, an appropriate skill exists, availability is confirmed, and no incompatible placement/reservation exists. Recheck at reservation time. Queue admission and selection are backend actions.

Flagged verification or non-active accounts must not be offered as eligible workers. Admin review does not bypass verification or availability. Concurrent requests must preserve single compatible reservation, as required by M0.1.

### Placement and replacement

Hiring/placement creation checks actual parties, ownership, candidate eligibility/availability, required approvals, and valid transitions. Replacement requests require all of:

1. The requesting employer owns the original placement.
2. The placement supports a replacement guarantee.
3. The request falls within the applicable guarantee period.
4. The unavailability reason and placement state meet policy.
5. No prohibited duplicate active request exists.

The 24-hour target is separate from guarantee coverage; timing, fulfillment, and escalation policy remain open from M0.1. Operational override means recorded investigation/retry/resolution through the workflow. It never permits choosing an ineligible worker, editing queue priority, ignoring duplicate reservations, or claiming completion without fulfillment.

### Referrals, operational corrections, and data access

Referrals require a relevant training recommendation/program and assigned duty. Operational status updates require evidence and allowed transitions. A candidate can communicate an update if a later workflow permits it, but cannot directly set official operational status.

Every operational correction records actor, affected entity, reason, and outcome. A need-to-know check applies even to ADMIN. Read/download permission must not be inferred from write permission or from owning an unrelated resource.

## 11. Sensitive data classification

| Category | Examples | Access boundary |
| --- | --- | --- |
| Public / low sensitivity | Display name where applicable, professional skills, job title, public company information | Public only when explicitly published/eligible; low sensitivity alone is not public consent |
| User-private | Personal profile details, bookings, application history, personal notifications | Owner/participants; narrowly authorized operational access |
| Employer-safe candidate information | Relevant skills, experience, portfolio, released score, verification status, availability | Eligible employer in hiring context through a filtered projection |
| Restricted verification data | NID-related information, identity documents, sensitive verification notes, voice records/metadata, emergency contacts | Minimum needed by assigned verification evaluator or authorized operational admin; separate limited subject view for policy-permitted own submissions |
| System-sensitive | Password hashes, JWT secrets, environment credentials, security configuration | Never ordinary APIs/dashboards, even for ADMIN; infrastructure-secret controls remain separate |

Use fictional identities in development/demo data. Do not retain raw audio solely to implement speech entry. Employer-safe status is a platform review outcome, not a claim of government validation. Final collection, retention, consent, self-access, release, and redaction fields must be resolved before real sensitive data is handled.

## 12. Multi-role policy

The architecture may support multiple roles through user_roles; do not impose exactly one role at database level. Typical MVP onboarding assigns a single operational role: CANDIDATE, EMPLOYER, EVALUATOR, or ADMIN.

Additional roles require trusted administrative assignment and a business reason. Membership is not automatic role inheritance: ADMIN does not automatically own a candidate profile or employer company. Separate roles do not erase ownership, duty assignment, account-status restrictions, or conflict-of-interest constraints. A candidate/evaluator cannot approve their own verification or score their own attempt.

How users select an operational context and how conflicts between multiple memberships are resolved must be specified before multi-role features are implemented. No multi-role code is created in M0.2.

## 13. Account status vs role

| Example | Interpretation |
| --- | --- |
| EMPLOYER + ACTIVE | Potentially permitted, subject to employer approval, ownership, and workflow rules |
| CANDIDATE + SUSPENDED | Role retained, but normal candidate operations are not authorized |
| EVALUATOR + BLOCKED | Role does not restore access to assignments |
| ADMIN + FLAGGED | Administrative membership does not bypass account review restrictions |

Canonical conceptual statuses remain ACTIVE, SUSPENDED, BLOCKED, FLAGGED. A valid role never overrides an inactive/restricted account status.

For this design, normal protected business operations require ACTIVE. SUSPENDED/BLOCKED accounts cannot continue them; FLAGGED accounts require review and do not participate in normal protected business workflows while flagged. Any limited account-status, recovery, appeal, or support access must be explicitly defined later; it is not an implicit role permission.

Authentication/token policy must eventually specify how status changes affect existing sessions. An existing token must not preserve authority contrary to the current status; no implementation mechanism is selected here.

## 14. Role change policy

Users cannot self-promote CANDIDATE → ADMIN, EMPLOYER → EVALUATOR, or grant additional memberships via account/profile edits. Role assignment/removal uses trusted, audited administrative operations. Initial admin provisioning, self-affecting admin role changes, and last-admin safeguards require an explicit policy in the identity module; bootstrap is not public registration.

Initial candidate onboarding selects TECH or TRADE. After type-specific assessments, verification, queue, placement, or other records exist, TECH ↔ TRADE is not a trivial UI toggle. Require validation/admin assistance, preserve history, and reconcile incompatible eligibility/reservations before a change is effective. Candidate type changes never assign platform roles.

Employer organization membership/delegation and approval criteria remain open. Until a policy authorizes organization sharing, no cross-employer access is inferred.

## 15. Future Spring Security mapping

Planning only: role checks may conceptually use hasRole("CANDIDATE"), hasRole("EMPLOYER"), hasRole("EVALUATOR"), and hasRole("ADMIN"), with method-level checks such as @PreAuthorize(...) where appropriate in a later module. These are notation, not implemented Java code or an API contract.

Future authorization combines authentication + account-status validation + role authorization + resource ownership/assignment + field visibility + business-rule validation.

For example, hasRole("EMPLOYER") is insufficient to edit a job; the backend must also resolve the current employer and verify that employer owns the job and the requested edit is allowed in its current state. Controllers, business services, restricted DTOs, and persistence queries must work together so direct API access cannot bypass UI restrictions.

No annotations, enums, entities, JWT libraries, guards, endpoints, or checks are implemented now.

## 16. Access-denied behavior

| HTTP status | Expected use |
| --- | --- |
| 401 Unauthorized | Missing or invalid authentication |
| 403 Forbidden | Authenticated identity lacks permission; includes direct candidate calls to admin-only operations |
| 404 Not Found | Missing resource, or a consistent ownership-sensitive policy conceals another user's private resource |
| 409 Conflict | Authorized action conflicts with resource state, such as a prohibited second active replacement request |

Use M0.1's centralized error conventions. Avoid leaking private resource existence, internal notes, or secrets in responses. An invalid role/ownership attempt must not receive detailed business-state errors before authorization is established. Exact resource-concealment and restricted-account authentication responses are implementation decisions to document consistently.

## 17. Assumptions, unresolved details, and review record

M0.2 inspection found only MASTER_SPEC.md in the repository; no README, docs directory, or application source existed. The complete master was reviewed. No conflict with approved M0.1 decisions was found. M0.1's instruction to stop at M0.1 was its historical scope boundary; the new user instruction explicitly authorizes this documentation module.

Design choices for least privilege:

- Admin-managed institutes remain partner entities, not a new login role.
- Assessment authoring defaults to ADMIN, with explicit scoped evaluator delegation possible.
- Evaluator verification decisions require an assigned approval duty.
- Employers initiate hiring/replacement requests; backend workflows produce official records.
- Admin queue oversight grants no arbitrary priority-edit permission.
- Normal protected actions require ACTIVE; limited recovery/appeal behavior is deferred.
- Own submitted-data views are distinct from restricted reviewer evidence endpoints.
- Existing M0.1 FIFO assumptions and guarantee-policy open questions remain unchanged.

Detailed approval criteria, organization membership, initial admin provisioning, field-level DTO/release policies, evaluator delegation, candidate-type reconciliation, and multi-role context handling must be decided before the affected implementation. These open details do not grant access by default.

Documentation verification should confirm all four roles, both candidate types, every matrix action, ownership rules, classifications, status restrictions, and invariants are present, and that MASTER_SPEC.md links here. No runtime security claims or application tests are made by this module.

## 18. Security Invariants

1. A Candidate cannot modify another Candidate's private data.
2. A Candidate cannot approve their own verification.
3. An Employer cannot modify another Employer's resources.
4. An Employer cannot manipulate assessment scores.
5. An Employer cannot directly manipulate waiting-list order.
6. An Evaluator cannot assign platform roles.
7. An Evaluator cannot manipulate unrelated employer resources.
8. Only authorized administrative operations may assign roles.
9. Sensitive verification data is never exposed through general candidate-search APIs.
10. Queue ordering is controlled by backend business logic.
11. Replacement requests require employer ownership and guarantee eligibility.
12. Audit records cannot be modified through normal user workflows.
13. Authorization must always be enforced by the backend.
14. Client-supplied user IDs never establish authorization.
15. Passwords, password hashes, JWT secrets, and infrastructure credentials are never exposed through ordinary application APIs or dashboards.
16. TECH and TRADE never grant authentication roles or privileged permissions.
17. Non-active account status cannot be overridden by role membership.
18. ADMIN access does not permit unvalidated direct database manipulation, audit erasure, or secret disclosure.
19. Additional role memberships do not permit self-verification, self-evaluation, or bypassing resource ownership.
20. A workflow trigger never grants arbitrary writes to authoritative scores, verification, queue, placement, or replacement state.

These statements are mandatory acceptance criteria for future implementation and security tests. M0.2 ends with documentation only; M0.3 is not part of this task.
