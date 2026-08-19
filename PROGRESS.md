# QuickTix - Project Development Progress Log

This log tracks all steps, setup actions, architectural decisions, and progress made on the **QuickTix** project.

---

#### Project initialization

- Initialized the backend Spring Boot project (quicktix)
- Created local PostgreSQL container setup so no manual local database installation is needed.
- created application.yml and application-dev.yml
- wrote Dockerfile and Docker compose file
- setup th local timezone to UTC and timezone configration in java code
- Added initial configuration for security in spring boot and implemented Basic Authentication and CORS configuration.
- added /health endpoint for liveness and readiness probes

---

#### Design model for differnt entity

