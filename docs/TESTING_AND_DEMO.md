# Testing and Demo Strategy

## 1. Philosophy

This project does not need production-grade exhaustive coverage.

Testing effort should protect:
- security boundaries;
- core business rules;
- replacement engine;
- important state transitions;
- demo reliability.

## 2. Backend Tests

High priority:

### Authentication
- duplicate registration;
- valid/invalid login;
- protected endpoint requires JWT;
- wrong role forbidden.

### Ownership
- employer cannot edit another employer's job;
- candidate cannot edit another candidate;
- employer cannot request replacement for another employer's placement.

### Assessment
- scoring;
- submitted attempt cannot be resubmitted incorrectly;
- strategy/factory selects correct implementation.

### Verification
- candidate cannot approve itself;
- evaluator can approve permitted case;
- employer-safe response excludes restricted notes.

### Booking
- capacity/conflict;
- cancellation.

### Queue/Replacement
- only eligible candidate enters/selects;
- FIFO order;
- double reservation prevented in the implemented transaction model;
- expired guarantee rejected;
- duplicate active request rejected;
- empty queue -> FAILED;
- SLA ON_TIME/BREACHED boundary.

### Observer
- key event creates notification/audit entry.

## 3. Repository Tests

Only where custom queries/constraints matter:
- FIFO queue query;
- active replacement detection;
- ownership query;
- unread notification.

Do not test trivial generated repository methods.

## 4. Frontend Tests

Do not spend large credits creating a huge UI test suite.

At minimum:
- TypeScript build;
- lint;
- important auth/form/component tests if test framework is established.

Manual browser testing is acceptable for visual showcase flows.

## 5. Commands

Backend from `backend/`:

```bash
./mvnw test
./mvnw clean package
```

Frontend after M1.3:

```bash
npm run lint
npm run build
```

Run development servers for integration checks as needed.

## 6. Manual End-to-End Checklist

### TECH
- register/login;
- complete profile;
- add skills;
- upload CV;
- assessment;
- evaluator result;
- employer search;
- shortlist;
- placement.

### TRADE
- register/login;
- trade category;
- manual onboarding;
- voice-assisted input;
- verification;
- HIRE_READY result;
- queue;
- placement;
- replacement.

### Replacement Demo
- employer opens active placement;
- replacement requested;
- timeline/countdown appears;
- candidate selected FIFO;
- notification appears;
- replacement completes;
- SLA result appears.

### Admin
- stats visible;
- verification/replacement operational view;
- account status action where implemented.

## 7. Final Demo Rehearsal

Before the project show:
- reset/load demo data;
- test all demo account credentials;
- test microphone/browser support;
- have manual fallback ready;
- verify no external network dependency is required for core flow;
- rehearse OOP pattern explanation;
- rehearse 5–8 minute primary story.

## 8. Project-Show Reliability

A locally running system with deterministic demo data is better than a fragile cloud deployment.

Do not risk the presentation for optional infrastructure.
