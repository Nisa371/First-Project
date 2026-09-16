# UI/UX Showcase Specification

## 1. Goal

The application should look like a polished modern talent/workforce product, not a generated CRUD admin panel.

Scope may be reduced, but visible quality must remain high.

## 2. Visual Direction

Recommended design language:

- primary deep navy/slate;
- indigo/blue primary action;
- teal/emerald accent for verified/success/workforce flows;
- warm light neutral page background;
- strong typography;
- generous whitespace;
- rounded cards;
- subtle borders/shadows;
- restrained gradients.

Avoid:
- excessive neon/glow;
- giant gradients everywhere;
- inconsistent card radii;
- dense tables on mobile;
- default browser forms;
- random icon sets;
- placeholder lorem ipsum.

A coherent Tailwind token approach is preferred.

## 3. Suggested Base Tokens

Exact implementation may adapt, but stay consistent.

```text
Background: slate-50 / near white
Primary text: slate-900
Muted text: slate-500/600
Primary action: indigo-600
Primary hover: indigo-700
Success/verified: emerald-600
Warning: amber-500/600
Danger: rose-600
Info: sky-600
Card: white
Border: slate-200
```

Use one consistent radius family, e.g. rounded-xl/2xl for major surfaces.

## 4. Typography

Prefer a clean sans-serif font.

Use:
- clear page titles;
- short supporting text;
- visually distinct labels/statuses;
- readable Bangla fallback/system font.

Do not overuse tiny gray text.

## 5. Public Landing Page

Must feel presentation-ready.

Recommended sections:
- hero with dual-track value proposition;
- TECH vs TRADE track cards;
- "How verification works";
- 24-hour replacement workflow illustration;
- employer benefit section;
- voice-accessibility highlight;
- trust/status metrics from fictional demo data;
- clear candidate/employer CTA.

Do not claim features that are not implemented.

## 6. Authentication

Login/register screens:
- branded split/card layout;
- candidate/employer registration choice;
- candidate TECH/TRADE choice;
- good validation;
- password visibility toggle if easy;
- loading state;
- friendly errors.

## 7. Dashboard Shell

Desktop:
- sidebar or strong top navigation;
- page title/breadcrumb area;
- notification control;
- user menu.

Mobile:
- compact navigation/drawer;
- no horizontal overflow;
- touch-friendly controls.

Role-specific navigation should not show irrelevant pages.

## 8. Candidate Dashboard

Show:
- greeting/profile status;
- candidate type badge;
- verification/readiness;
- skills;
- assessment status;
- next booking;
- placement status;
- referrals;
- notifications.

TRADE dashboard should additionally highlight:
- trade category;
- availability;
- queue status/position;
- voice onboarding shortcut.

## 9. Employer Dashboard

Show:
- active jobs;
- shortlisted candidates;
- active placements;
- replacement requests;
- recent activity.

Candidate search cards should clearly show:
- name;
- skills;
- location;
- verification badge;
- released readiness;
- availability.

## 10. Evaluator Dashboard

Show:
- pending assessments;
- pending verification;
- upcoming appointments;
- recent evaluations.

Use clear work-queue cards/table with status filters.

## 11. Admin Dashboard

Keep it visually strong but functionally small.

Use:
- stat cards;
- simple charts only if low-cost/useful;
- recent replacement activity;
- verification workload;
- user/account table.

Do not build enterprise admin complexity.

## 12. TRADE Onboarding

This is a showcase feature.

Requirements:
- mobile-first;
- minimal typing;
- large buttons;
- step/progress indicator;
- simple Bangla-friendly labels/help;
- trade category cards;
- voice microphone button;
- visible listening state;
- transcript preview;
- user can edit transcript;
- manual fallback always visible.

Voice failure must never block onboarding.

## 13. Assessment UI

Use:
- progress indicator;
- clear question card;
- option selection states;
- timer only if actual duration is enforced;
- submit confirmation;
- result summary.

Do not show answer keys.

## 14. Verification UI

Candidate:
- submission card;
- status timeline/chip;
- friendly explanation.

Evaluator:
- review panel;
- candidate summary;
- restricted info;
- approve/fail/flag actions;
- confirmation for sensitive action.

## 15. Waiting List UI

Make queue status visually impressive.

Candidate view can show:
- skill/category;
- status;
- joined time;
- approximate/actual position if safely calculable;
- availability toggle;
- "ready for placement" state.

Admin/demo view may show queue cards ordered FIFO.

## 16. Replacement UI — Hero Showcase

This is the most important visual workflow.

Employer replacement detail should show:
- original placement;
- reason;
- 24h countdown/progress;
- current status;
- selected replacement candidate;
- event timeline.

Suggested timeline:

```text
Request received
→ Matching queue
→ Candidate selected
→ Accepted
→ Replacement active
```

Use status chips and visually distinct SLA result:
- PENDING
- ON_TIME
- BREACHED

For demo, animate transitions lightly with CSS/React state where appropriate, but do not add heavy animation libraries unless justified.

## 17. Notification Center

Simple bell + list.

Each item:
- icon/type;
- title;
- time;
- unread marker;
- action/link where useful.

## 18. Loading / Error / Empty States

Every major data page must have:
- loading skeleton/spinner;
- friendly error;
- deliberate empty state;
- retry/action where useful.

Never leave a blank page.

## 19. Accessibility

At minimum:
- semantic labels;
- keyboard usable controls;
- focus visibility;
- adequate contrast;
- buttons not made from unlabelled divs;
- form errors associated with inputs;
- responsive touch targets.

## 20. Responsive Expectations

The final demo must work at:
- desktop/laptop;
- common mobile width.

TRADE workflows are especially important on mobile.

## 21. Demo Data

Use realistic fictional:
- people;
- company names;
- job titles;
- skills;
- assessment results;
- queue entries;
- placements;
- notifications.

A visually empty app is not showcase-ready.

## 22. UI Definition of Done

A user-facing module is not complete merely because API data renders.

It should:
- match the shared visual language;
- work responsively;
- handle loading/error/empty states;
- have clear success/validation feedback;
- avoid obvious placeholder styling.
