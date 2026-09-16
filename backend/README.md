# Backend Workspace

The Spring Boot application root is `backend/`. M1.2–M1.5 provide the health endpoint, environment profiles and browser CORS foundation; M2.1–M2.5 add the 21-entity persistence foundation; M3 adds authentication and shared API/security infrastructure.

Requirements: **JDK 21** and network access for the first Maven dependency download. Spring Boot **4.1.1** uses Maven **3.9.16** through the included Apache Maven Wrapper **3.3.4** (only-script distribution). Global Maven is unnecessary. Set `JAVA_HOME` to a JDK 21 installation if the default `java` is another version.

Coordinates: `com.marketplace:verified-career-marketplace-backend:0.0.1-SNAPSHOT`, packaging `jar`. The snapshot label is the local application's development version; Spring Boot and JJWT use stable releases. Package root is `com.marketplace`; entry point is `VerifiedCareerMarketplaceApplication`.

Run from `backend/` on Linux/macOS:

```bash
./mvnw test
./mvnw clean package
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd` for the same goals. The executable JAR is `target/verified-career-marketplace-backend-0.0.1-SNAPSHOT.jar`. Stop the running application with Ctrl+C. Inspect dependencies with `./mvnw dependency:tree`.

Local address: `http://localhost:8080`; API base `/api`. Health check:

```bash
curl -i http://localhost:8080/api/health
```

Expected: HTTP 200, JSON `{"status":"UP"}`, without authentication. This confirms the backend can answer a request; it does not probe database readiness.

### Database and authentication

`application.yml` holds shared configuration and defaults to `dev`:

- `application-dev.yml`: ephemeral H2; no external database; default CORS origin `http://localhost:5173`.
- `application-test.yml`: separate ephemeral H2 database; tests explicitly select `test`.
- `application-prod.yml`: MySQL from required `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`; schema validation only. Supply `CORS_ALLOWED_ORIGINS` to allow browser access; otherwise no origins are allowed.

Set `SPRING_PROFILES_ACTIVE` to choose a profile and `SERVER_PORT` to override 8080. Spring Boot reads exported process variables; it does not automatically load `.env`. See `.env.example` for names. Dev/test use `ddl-auto: create-drop` to create the entity schema on startup and discard it on shutdown; demo data is ephemeral. Production expects an already provisioned matching schema. H2 console and SQL initialization remain disabled.

CORS accepts exact comma-separated HTTP(S) origins, rejects wildcard/path values, and permits GET/POST/PUT/PATCH/DELETE/OPTIONS and Accept/Content-Type/Authorization headers under `/api/**`. It does not enable cookies or grant API authorization. Use `localhost:5173` consistently; a browser opened at `127.0.0.1:5173` requires a separately configured origin.

Public routes are GET `/api/health` and POST `/api/auth/register` and `/api/auth/login`. GET `/api/auth/me` returns the current safe user summary. Other routes require a valid Bearer JWT and an ACTIVE account; role gates protect admin, evaluator and self-profile namespaces. Services use `CurrentAccount` for ownership and `@PreAuthorize` where needed. Authentication is stateless with CSRF disabled because tokens are supplied explicitly in Authorization headers, not cookies.

Set `JWT_SECRET` to a Base64-encoded random key of at least 32 bytes (generate with `openssl rand -base64 32`). It is required outside dev/test, including any configuration with prod active. Dev/test can omit it to generate a random process-local key; backend restarts invalidate those sessions. `JWT_TTL_SECONDS` defaults to 3600 (60–86400 allowed). No refresh tokens or seeded privileged accounts are provided. Public registration permits CANDIDATE/EMPLOYER only; passwords use BCrypt. Unknown request fields are rejected. See `docs/API_SPEC.md` for payloads.

### Current scope and dependencies

Implemented: Spring Boot bootstrap, MVC/REST foundation, JPA and validation dependencies, MySQL driver, H2 development bootstrap, Spring Security JWT authentication, role gates and safe errors, typed health response and context/HTTP security tests.

Direct dependencies (versions managed by the Boot parent unless specified):

| Group | Artifact | Scope |
| --- | --- | --- |
| org.springframework.boot | spring-boot-starter-webmvc | compile |
| org.springframework.boot | spring-boot-starter-data-jpa | compile |
| org.springframework.boot | spring-boot-starter-validation | compile |
| org.springframework.boot | spring-boot-starter-security | compile |
| com.mysql | mysql-connector-j | runtime |
| com.h2database | h2 | runtime |
| org.projectlombok | lombok | optional compile; annotation processor, excluded from packaged runtime |
| io.jsonwebtoken | jjwt-api | compile; 0.13.0 |
| io.jsonwebtoken | jjwt-impl | runtime; 0.13.0 |
| io.jsonwebtoken | jjwt-jackson | runtime; 0.13.0 |
| org.springframework.boot | spring-boot-starter-test | test |
| org.springframework.boot | spring-boot-starter-webmvc-test | test; Boot 4 MVC/MockMvc auto-configuration |
| org.springframework.security | spring-security-test | test |

M2 adds feature-oriented entities/repositories, string enums, foreign keys, uniqueness/check constraints, timestamps, FIFO/ownership/unread queries and transaction-locking hooks.

Not implemented: later domain services/APIs, migrations or deployment. JJWT’s Jackson 2 adapter handles JWT claims separately from Boot 4’s Jackson 3 MVC mapper.

Backend owns database and CORS settings. Frontend owns its public `VITE_API_BASE_URL`. JWT configuration stays backend-only. Real environment files are ignored; only examples should be committed.

Follow [architecture](../docs/ARCHITECTURE.md), [standards](../docs/DEVELOPMENT_STANDARDS.md), [API contracts](../docs/API_SPEC.md), [workflows](../docs/BUSINESS_WORKFLOWS.md) and [security](../docs/SECURITY.md).

### M1.2 validation record

On 2026-09-12, the environment's default OpenJDK was 17.0.20 and global Maven was absent. Validation selected checksum-verified Temurin 21.0.12.1 from a workspace-local JDK without changing system software. Maven needed temporary environment-specific proxy settings and the system trust store; these are not application configuration or repository files.

| Check | Result |
| --- | --- |
| `./mvnw test` | PASS: 3 tests, 0 failures/errors/skips |
| `./mvnw clean package` | PASS: executable JAR; Java 21 and correct start class in manifest |
| `./mvnw dependency:tree` | PASS: Boot-managed versions and JJWT 0.13.0 reviewed |
| `./mvnw spring-boot:run` | PASS: port 8080, H2, no external MySQL |
| Anonymous GET `/api/health` | 200, application/json, exactly `{"status":"UP"}` |
| Anonymous GET `/error` | 401 |
| Shutdown | Graceful Tomcat, JPA and connection-pool shutdown |

The commands used the temporary Maven `-s` settings path and `JAVA_HOME` selection for this environment. Mockito's inherited test listener emitted its standard self-attachment warning; tests and build passed. No generated-user password warning appeared. Source/link/ignore checks passed, wrapper files are tracked and `target/` is ignored. The historical M1.2 checks did not cover JWT; M3 adds authentication/security tests.

### M1.4–M1.5 validation

On 2026-09-13, `./mvnw clean package` passed with 7 tests. Default-dev executable-JAR startup used H2 without MySQL. Direct health returned 200; browser health connectivity, allowed preflight, untrusted-origin rejection and the protected error route passed. Production MySQL configuration is retained but no external MySQL instance was exercised.

## Profiles, hiring and CV storage (M4)

Candidate/employer self-service, employer-owned jobs, filtered talent search and shortlists are available through the contracts in `docs/API_SPEC.md`. Startup supplies eight starter skills without creating demo accounts.

TECH CVs accept PDF files up to 5 MB. Set `CV_STORAGE_DIRECTORY` to a private writable directory (default `uploads/cv`, ignored by Git). Keep it outside frontend/static assets. Downloads require authentication; successful replacement removes the superseded file. PDF type/signature validation is not malware scanning.

## Assessment and evaluator demo (M5)

Startup seeds two explicitly labeled, untimed assessments: TECH foundations (automatic weighted MCQ scoring) and a Bangla/English TRADE safety conversation (manual evaluator scoring). Set `APP_ASSESSMENTS_SEED_DEMO=false` to disable these seeds. Existing assessment titles are not overwritten. The MVP allows one saved attempt per candidate/assessment.

For a local evaluator login, use the `dev` profile and export `DEMO_EVALUATOR_EMAIL` and `DEMO_EVALUATOR_PASSWORD` before startup. Choose your own email and a password of at least 10 characters; no credentials are shipped. The initializer stores a BCrypt hash, never changes an existing account’s role/password, and is disabled outside dev. Spring Boot does not automatically load `.env` files.

Demo: register a TECH candidate → Assessments → answer/save/submit → sign in as the configured evaluator → Work queue → save score/feedback/recommendation → release result → publish a future appointment slot → return as candidate to view the result and book/cancel an appointment. Private drafts and evaluator notes remain hidden from candidates/employers. TRADE answers use manual scoring; voice capture remains M6.

## Populated fictional showcase (M8)

Use the `dev` profile with `DEMO_ENABLED=true` and supply `DEMO_PASSWORD` privately
in the process environment (10–72 UTF-8 bytes). There is no default password and
no committed credential. Environment example files are not automatically loaded.
All showcase accounts share the supplied password and use these reserved emails:

| Account | Email | Starting scene |
| --- | --- | --- |
| Admin | admin@showcase.example.test | Account, verification, jobs and skill overview |
| Evaluator | evaluator@showcase.example.test | Pending review, appointments and training referral |
| Field employer | employer@showcase.example.test | Selected replacement ready to confirm/activate |
| Tech employer | studio@showcase.example.test | Jobs, shortlist and active placement |
| TRADE workers | trade1@showcase.example.test through trade6@showcase.example.test | Original placement, reserved replacement, two queued workers, training referral, pending verification respectively |
| TECH candidates | tech1@showcase.example.test, tech2@showcase.example.test | Active placement and available candidate respectively |

Names, companies and evidence references are fictional. No real phone numbers,
NID numbers or CV documents are seeded. Dates are relative to startup so the
replacement has 23 hours remaining and the appointment is tomorrow. Ordering and
relationships are deterministic. A transaction seeds all records once; rerunning
preserves user edits. The default in-memory dev database resets at application
restart, which resets the demo too. Never enable this fixture for production;
the initializer is restricted to `dev` and is off by default.
