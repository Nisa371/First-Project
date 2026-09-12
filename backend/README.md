# Backend Workspace

The Spring Boot application root is `backend/`. M1.2 adds the infrastructure foundation; business functionality remains planned.

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

### Database and temporary security

`src/main/resources/application.yml` explicitly selects an in-memory H2 database for temporary development/test bootstrap. No external MySQL server is needed. H2 data is nonpersistent; Hibernate DDL generation is `none`, SQL initialization is disabled, and no entities/schema/seeds exist. The H2 console is disabled and its Boot console module is absent. MySQL remains the target persistent database; production configuration and environment profiles are deferred to M1.4/M2.

Temporary security allows only GET `/api/health` anonymously; other routes require authentication. No authentication mechanism is implemented, so protected routes are unavailable to anonymous clients (401; unsafe requests without CSRF tokens may receive 403). Spring's generated in-memory user auto-configuration is excluded. There is no form login, Basic login setup, user service, JWT filter or demo account. CSRF remains enabled; no CORS configuration is added. M4 owns final JWT authentication, RBAC and stateless security. M3.2 owns the complete error contract; this baseline does not implement a global exception framework.

### Current scope and dependencies

Implemented: Spring Boot bootstrap, MVC/REST foundation, JPA and validation dependencies, MySQL driver, H2 development bootstrap, Spring Security dependency/temporary baseline, typed health response and context/HTTP security tests.

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

Not implemented: authentication/JWT logic, users/roles, domain entities, repositories, business services/APIs, frontend, production MySQL configuration, migrations or deployment. JJWT is reserved for M4; its Jackson 2 adapter is separate from Boot 4's Jackson 3 MVC stack. No shared mapper or JWT parsing behavior is assumed.

Backend owns DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, FRONTEND_URL and CORS_ALLOWED_ORIGINS. Root `.env.example` is a temporary conceptual reference, not an automatically loaded configuration. Application-specific examples can be added when needed. Never commit real secrets. Future configurable uploads may use ignored `backend/uploads/`; generated builds belong in ignored `backend/target/`.

Follow [architecture](../docs/ARCHITECTURE.md), [standards](../docs/DEVELOPMENT_STANDARDS.md), [API contracts](../docs/API_SPEC.md), [workflows](../docs/BUSINESS_WORKFLOWS.md) and [access rules](../docs/ROLES_AND_PERMISSIONS.md). Shared specifications remain under `docs/` and [MASTER_SPEC.md](../MASTER_SPEC.md).

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

The commands used the temporary Maven `-s` settings path and `JAVA_HOME` selection for this environment. Mockito's inherited test listener emitted its standard self-attachment warning; tests and build passed. No generated-user password warning appeared. Source/link/ignore checks passed, wrapper files are tracked and `target/` is ignored. Future M4 JWT tests must verify token behavior; M1.2 does not claim it.
