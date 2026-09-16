# Security & Permissions

## 1. Philosophy

The project needs credible security, not enterprise security infrastructure.

Backend rules are authoritative.

## 2. Roles

- CANDIDATE
- EMPLOYER
- EVALUATOR
- ADMIN

Candidate type:
- TECH
- TRADE

One primary operational role per account is sufficient for the revised MVP.

## 3. Candidate

Can:
- manage own editable profile;
- manage own skills;
- upload own TECH CV;
- take own assessments;
- view own released results;
- book own eligible appointment;
- submit/view own verification;
- view own placement/referrals/notifications;
- see own queue status if TRADE.

Cannot:
- access another candidate's private data;
- set role/account status;
- self-verify;
- set score/recommendation;
- set queue priority;
- create arbitrary placement/replacement states;
- view audit logs.

## 4. Employer

Can:
- manage own company;
- manage own jobs;
- search employer-safe eligible candidates;
- shortlist;
- view own placements;
- request replacement for own eligible placement;
- view own replacement status.

Cannot:
- edit another employer's job;
- modify candidate profile;
- modify scores;
- view raw verification evidence;
- manipulate queue order;
- change authoritative replacement state arbitrarily.

## 5. Evaluator

Can:
- review eligible assessment attempts;
- score/recommend;
- review verification cases;
- add feedback/internal notes;
- manage own/authorized slots;
- create training referral where appropriate.

Cannot:
- assign admin role through public flow;
- manipulate queue priority;
- fabricate placement;
- expose restricted evidence.

## 6. Admin

Can:
- inspect/manage user account status;
- inspect verification/replacement operations;
- manage reference/demo data where required;
- view simple stats;
- inspect audit logs if UI is implemented.

Admin actions should still use services and create audit entries.

## 7. Sensitive Data

Restricted:
- passwordHash;
- verification evidence;
- identity numbers/references;
- internal evaluator notes;
- JWT secret;
- DB credentials.

Employer-safe candidate DTO may contain:
- name;
- skills;
- relevant experience;
- CV/portfolio;
- released recommendation/score;
- verification status;
- availability.

## 8. JWT

- backend-only signing secret;
- environment variable;
- no refresh token required;
- no secret in frontend;
- invalid/expired token rejected.

## 9. Ownership

Prefer self endpoints:

```text
/candidates/me
/employers/me
/bookings/me
/notifications/me
```

For ID-based resources:
- load resource;
- compare authenticated owner/authorized role;
- then perform action.

## 10. File Upload

For CV:
- size cap;
- allowlisted content types/extensions;
- generated safe stored name;
- no path traversal;
- upload directory ignored by Git.

## 11. Verification

This is platform/manual verification.

Do not label it:
- government verified;
- NID API verified;
- police verified;

unless a real integration exists.

Use fictional data in demo.

## 12. CORS

Development allow origin:
- frontend local URL configured in environment.

Do not use broad wildcard as a shortcut for the final demo setup.

## 13. High-Value Security Tests

At minimum:
- unauthenticated protected endpoint;
- candidate blocked from admin/evaluator action;
- employer blocked from another employer's job;
- employer blocked from wrong placement replacement;
- candidate blocked from self-verification;
- employer-safe candidate response excludes restricted fields.
