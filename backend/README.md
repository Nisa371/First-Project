# Backend Workspace

The Spring Boot application root is `backend/`. Run backend commands here once M1.2 initializes Maven and the application. Planned stack: Java, Spring Boot/MVC, Maven, Spring Data JPA, Spring Security, Bean Validation, MySQL, H2, Lombok and JWT authentication (M4).

Metadata: group/package root `com.marketplace`, artifact/name `verified-career-marketplace-backend`. Maven files (`pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`) and `src/` belong here, never in a nested application or at repository root. Runtime YAML belongs in `src/main/resources/`. Create only packages actually needed.

Expected local address: `http://localhost:8080`; API base `/api`. M1.2 implements the planned public `GET /api/health` availability response `{"status":"UP"}`. M1.4 owns environment/profile/CORS setup; MySQL is the persistent target, H2 is optional development/test support.

Backend owns DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, FRONTEND_URL and CORS_ALLOWED_ORIGINS. Root `.env.example` is a temporary conceptual reference, not an automatically loaded configuration. Application-specific examples can be added when needed. Never commit real secrets. Future configurable uploads may use ignored `backend/uploads/`; generated builds belong in ignored `backend/target/`.

Follow [architecture](../docs/ARCHITECTURE.md), [standards](../docs/DEVELOPMENT_STANDARDS.md), [API contracts](../docs/API_SPEC.md), [workflows](../docs/BUSINESS_WORKFLOWS.md) and [access rules](../docs/ROLES_AND_PERMISSIONS.md). Shared specifications remain under `docs/` and [MASTER_SPEC.md](../MASTER_SPEC.md).
