# QuickTix - Project Development Progress Log

This log tracks all steps, setup actions, architectural decisions, and progress made on the **QuickTix** project.

---

#### Project initialization (day 1)

- Initialized the backend Spring Boot project (quicktix)
- Created local PostgreSQL container setup so no manual local database installation is needed.
- created application.yml and application-dev.yml
- wrote Dockerfile and Docker compose file
- setup th local timezone to UTC and timezone configration in java code
- Added initial configuration for security in spring boot and implemented Basic Authentication and CORS configuration.
- added /health endpoint for liveness and readiness probes

---

#### Design model for different entity (day 2)

- designed the user entity with (id, name, email, password, passwordHash, role, createdAt, updatedAt)
- designed the enum class for the role of the user
- designed the venue entity having (id, name, address, totalCapacity, organizer, createdAt, updatedAt)
- designed the Event entity having (id, title, description, venue, start time, category, baseprice, organiserid, createdat)

