# QuickTix - Project Development Progress Log

This log tracks all steps, setup actions, architectural decisions, and progress made on the **QuickTix** project.

---

- Initialized the backend Spring Boot project (`quicktix`).
- **Dependencies Added**:
  - Spring Web (MVC & RESTful APIs)
  - Spring Data JPA (Database ORM & Repositories)
  - PostgreSQL Driver (Database Connector)
  - Validation (Bean validation annotations `@NotNull`, `@Size`, etc.)
  - Spring Security (Authentication & Authorization framework)
  - Lombok (Boilerplate reduction annotations)
  - Spring Boot DevTools (Hot reload & dev utilities)

---

- **Action**: Created local PostgreSQL container setup so no manual local database installation is needed.
- **Details**:
  - Image: postgres:16
  - Container Name: quicktix-postgres
  - Exposed Port: 5432
  - Database Name: quicktix_db
  - Credentials: postgres / postgres
  - Storage: Configured named persistent volume `postgres_data` so data survives container restarts.

---

- **Action**: Configured Spring Boot configuration profiles using YAML instead of properties.
- **Details**:
  - application.yml: Set active profile to dev by default.
  - application-dev.yml: Database connection parameters with environment variable overrides (DB_URL, DB_USERNAME, DB_PASSWORD) and sensible defaults for local development (jdbc:postgresql://localhost:5432/quicktix_db).
  - Configured Hibernate ddl-auto: update and enabled SQL logging (show-sql: true).

---

- **Action**: Added initial health check controller and security configuration with beginner vs standard code documentation.

---


