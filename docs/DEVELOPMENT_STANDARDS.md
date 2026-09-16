# Development Standards

## 1. Engineering Goal

Build a clean, understandable academic MVP.

Prefer:
- simple;
- explicit;
- testable;
- consistent.

Avoid:
- enterprise architecture;
- unnecessary abstractions;
- speculative infrastructure.

## 2. Source of Truth

Priority:

1. current explicit user instruction;
2. `MASTER_SPEC.md`;
3. module requirements in `IMPLEMENTATION_PLAN.md`;
4. detailed docs;
5. existing working code;
6. README operational notes.

If code and docs disagree, inspect before rewriting. Preserve working completed behavior unless the current task authorizes a change.

## 3. Java

- Java 21 baseline.
- UTF-8.
- four-space indentation.
- PascalCase classes.
- camelCase methods/fields.
- UPPER_SNAKE_CASE enums/constants.
- constructor injection.
- avoid static mutable state.

## 4. Spring

Controller:
- request/response only.

Service:
- business rules, ownership, transactions.

Repository:
- persistence queries.

Use:
- DTOs;
- Bean Validation;
- `@Transactional` where atomicity is required.

Do not:
- expose entity directly;
- bind client JSON directly to authoritative entity fields;
- write business logic in controller.

## 5. Error Handling

Use central safe errors once M3 introduces it.

Machine field:

```text
error
```

not `code`, unless an approved change later updates the spec.

Do not return stack traces/SQL details.

## 6. Database

- snake_case table/column names if explicitly named;
- enum strings rather than ordinals;
- real FK relations;
- useful uniqueness constraints;
- no universal soft-delete framework;
- no performance indexing spree before real queries exist.

## 7. Frontend

- TypeScript;
- functional React;
- hooks;
- feature-oriented services/components;
- central Axios config;
- typed API responses;
- no widespread `any`;
- Tailwind for styling;
- route layouts instead of repeated navigation.

## 8. Frontend State

Use local React state/context when sufficient.

Do not add Redux/Zustand/TanStack Query merely by default.

A library may be added only if the implemented feature genuinely benefits and the dependency cost is justified.

## 9. Forms

Use controlled/simple forms or an approved form library if complexity warrants it.

Always:
- client validation for UX;
- backend validation for authority;
- clear field errors.

## 10. Dependencies

Before adding a dependency ask:
- can the existing stack solve this cleanly?
- does it materially improve showcase quality or implementation reliability?

Good low-cost UI dependency if needed:
- `lucide-react` for coherent icons.

Avoid large UI frameworks that fight the Tailwind design.

## 11. Environment

Backend secrets:
- env/config only.

Frontend:
- `VITE_API_BASE_URL`;
- no secrets.

Track:
- `.env.example`.

Ignore:
- real `.env`;
- uploads;
- build outputs.

## 12. Git

Before task:
- `git status`;
- inspect diff.

After task:
- show concise status;
- do not commit unrelated generated junk.

Commit format recommended:

```text
feat:
fix:
docs:
test:
refactor:
chore:
style:
```

## 13. Testing

During a multi-module batch:
- targeted tests after meaningful components;
- broader backend/frontend build/test at batch end.

Do not run the full suite after every trivial file edit.

See `TESTING_AND_DEMO.md`.

## 14. Documentation

Do not rewrite documents just to restate code.

Update:
- `PROGRESS.md` after module completion;
- implementation/spec docs only when a decision actually changed.

## 15. No Fake Completion

A feature is not implemented if it is:
- a TODO;
- a mocked button;
- a hard-coded success response;
- a static screenshot;
- a UI without the required API when the module requires integration.

Demo seed data is allowed and encouraged, but must be clearly fictional.

## 16. Scope

Do not implement STRETCH work unless explicitly requested.

Do not redesign completed architecture as part of a normal feature task.
