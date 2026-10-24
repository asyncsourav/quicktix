# Concurrency Control & Double-Booking Prevention

**Objective:** Guarantee zero double-bookings during concurrent ticket drops without database deadlocks.

---

## Locking Strategy Decision

| Architectural Metric | Pessimistic Locking (`SELECT FOR UPDATE`) | My Choice: Optimistic Locking (`@Version`) |
| :--- | :--- | :--- |
| **Locking Model** | Holds row-level database locks for entire transaction | Holds no database locks; validates version at commit |
| **Deadlock Risk** | High when transactions lock multiple seats in varying order | **Zero deadlock risk** (no rows locked) |
| **Throughput** | Bottlenecked as threads queue for locks | **Maximum throughput**; non-conflicting checkouts run in parallel |
| **Connection Pool** | Keeps HikariCP connections occupied waiting | Releases connections rapidly |

---

## Concurrency Pipeline & SQL Predicate

1. **Entity Versioning ([`Seat.java`](../server/src/main/java/com/asyncsourav/quicktix/entity/Seat.java)):**
   ```java
   @Version
   private Long version;
   ```
2. **Transactional Boundary ([`BookingService.java`](../server/src/main/java/com/asyncsourav/quicktix/service/BookingService.java)):**
   Reservation executes inside `@Transactional`.
3. **Atomic SQL Predicate:**
   ```sql
   UPDATE seats
   SET status = 'BOOKED', version = 2, updated_at = NOW()
   WHERE id = 10 AND version = 1;
   ```
4. **Collision Resolution:**
   * **Winner Thread:** Updates row, increments version from `1` to `2` (1 row affected). Returns **HTTP 201 Created**.
   * **Loser Thread:** Executes `WHERE id = 10 AND version = 1` against already-updated row (0 rows affected). Hibernate throws `ObjectOptimisticLockingFailureException`.
   * **Atomic Rollback:** Spring catches the exception and rolls back the loser's transaction completely.

---

## HTTP 409 Collision Response

Translated by [`GlobalExceptionHandler.java`](../server/src/main/java/com/asyncsourav/quicktix/exception/GlobalExceptionHandler.java):

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

## Benchmark Verification ([`scripts/race.mjs`](../scripts/race.mjs))

10 concurrent requests fired targeting the exact same seat within the same millisecond:

* **Dispatched Requests:** 10 simultaneous HTTP requests.
* **Confirmed Bookings:** **1** (HTTP 201 Created).
* **Rejected Collisions:** **9** (HTTP 409 Conflict).
* **Database State:** Exactly 1 booking saved; seat status `BOOKED`; version incremented to `2`; **zero double-bookings**.
