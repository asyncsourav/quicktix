# Concurrency Control & Double-Booking Prevention

This document details how I designed and verified QuickTix's database isolation mechanism to completely eliminate seat over-selling and race conditions during high-volume ticket drops.

---

## The Core Challenge I Had to Solve

In high-demand ticket drops, thousands of users hit the purchase button at the exact same millisecond. 

Early on, I knew that a naive application-level check would catastrophically fail:
```java
// Vulnerable to race conditions
if (seat.getStatus() == SeatStatus.AVAILABLE) {
    seat.setStatus(SeatStatus.BOOKED);
    seatRepository.save(seat);
}
```
Under concurrent traffic, Thread A and Thread B read `AVAILABLE` simultaneously before either transaction commits. Both threads proceed to create a booking, causing a double-booked seat and corrupted business state.

---

## Why I Chose Optimistic Locking Over Pessimistic Locking

When deciding how to prevent race conditions at the database layer, I evaluated both Pessimistic and Optimistic locking:

| Architectural Metric | Pessimistic Locking (`SELECT FOR UPDATE`) | My Decision: Optimistic Locking (`@Version`) |
| :--- | :--- | :--- |
| **Locking Strategy** | Holds row-level database locks for the entire duration of the transaction | Holds no database locks; checks record version at commit time |
| **Deadlock Risk** | High when reserving multiple seats in different order across transactions | **Zero deadlock risk** because no row-level database locks are held |
| **System Throughput** | Degrades rapidly under heavy read/write load as threads wait on locks | **Maximum throughput**; non-conflicting reservations proceed concurrently |
| **HikariCP Pool Usage** | Keeps connections occupied waiting for lock releases | Releases connections rapidly, maximizing connection pool efficiency |

Because ticket drops are read-heavy with localized bursts of contention, I chose **JPA/Hibernate Optimistic Concurrency Control** via a version column on my [`Seat.java`](../server/src/main/java/com/asyncsourav/quicktix/entity/Seat.java) entity:

```java
@Entity
@Table(name = "seats")
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    @Version
    private Long version;
    ...
}
```

---

## How I Engineered the Locking & Rollback Pipeline

1. **Transactional Boundary:** I wrapped the entire reservation workflow in [`BookingService.java`](../server/src/main/java/com/asyncsourav/quicktix/service/BookingService.java) inside a Spring `@Transactional` boundary.
2. **Read Phase:** When an attendee initiates checkout, I fetch the requested seats and their current versions (e.g., `id = 10, version = 1`).
3. **Predicate-Level SQL Write:** When updating the seat status, Hibernate issues a SQL query with a version predicate:
   ```sql
   UPDATE seats
   SET status = 'BOOKED', version = 2, updated_at = NOW()
   WHERE id = 10 AND version = 1;
   ```
4. **Collision Handling:**
   * **Winner Thread:** Executes first, updates the row, and increments `version` from `1` to `2`. Exactly 1 row is affected.
   * **Loser Thread:** Attempts to execute the update `WHERE id = 10 AND version = 1`. Because the version is already `2`, the update matches 0 rows.
5. **Atomic Batch Rollback:** Hibernate detects 0 updated rows and throws `ObjectOptimisticLockingFailureException`. Spring's transaction manager catches this and immediately triggers a complete, atomic rollback of the entire booking transaction.
6. **HTTP 409 Conflict Translation:** In [`GlobalExceptionHandler.java`](../server/src/main/java/com/asyncsourav/quicktix/exception/GlobalExceptionHandler.java), I translate this collision into a clean HTTP 409 response:
   ```json
   {
     "timestamp": "2026-10-24T08:15:00.124",
     "status": 409,
     "error": "Conflict",
     "message": "This seat was just selected or booked by another user. Please choose an available seat.",
     "path": "/api/bookings"
   }
   ```

---

## How I Verified This with `race.mjs`

To physically prove this mechanism under genuine race conditions, I wrote [`scripts/race.mjs`](../scripts/race.mjs) using `Promise.all` to fire 10 simultaneous asynchronous requests targeting the exact same seat within the same millisecond:

* **Total Dispatched Requests:** 10
* **Successful Confirmations:** 1 (HTTP 201 Created)
* **Rejected Collisions:** 9 (HTTP 409 Conflict)
* **Database State:** Exactly 1 booking was created in PostgreSQL; the seat status changed to `BOOKED`; the version incremented from `1` to `2`. Zero double-bookings occurred.
