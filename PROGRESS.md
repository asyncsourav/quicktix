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
- designed the Seat entity having (id, event, seatLable, price, seatstatus, version, booking, createdAt, updatedAt)
- designed the enum class for the seat status

```
What does the version and seatStatus does ?? why use both ??


                                            [ DATABASE ]
                                    Seat A1: status = AVAILABLE
                                                version = 0
                                                    │
                            ┌───────────────────────┴───────────────────────┐
                            ▼                                               ▼
                    [ SOURAV'S THREAD ]                             [ ALEX'S THREAD ]
            1. Reads Seat A1 (version = 0)                  1. Reads Seat A1 (version = 0)
            2. Changes status to 'HELD'                     2. Changes status to 'HELD'
            3. Sends SQL UPDATE to DB:                      3. Sends SQL UPDATE to DB (1ms later):
                UPDATE seats                                    UPDATE seats
                SET status = 'HELD', version = 1                SET status = 'HELD', version = 1
                WHERE id = 10 AND version = 0;                  WHERE id = 10 AND version = 0;
                            │                                               │
                            ▼                                               ▼
                MATCH! (DB version was 0)                    NO MATCH! (DB version is now 1)
                -> Rows updated: 1                              -> Rows updated: 0
                -> DB version becomes 1                         -> Hibernate throws OptimisticLockException!
                -> SOURAV GETS THE SEAT!                        -> ALEX'S TRANSACTION ROLLS BACK!
```
- booking entity and booking status enum class completed 
- booking entity has (id, user (ManyToOne), event, List<Seat> (OneToMany), totalAmount, status(enum), created at, updated at)

---

#### Design the repository class (day 3)

- User repo (UserRepository)
- Venue repo (VenueRepository)
- Evenet Repository
- Seat Repository
- Booking Repository

