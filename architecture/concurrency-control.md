# Concurrency Control & Double-Booking Prevention

This document details QuickTix's database isolation mechanism designed to eliminate seat over-selling and race conditions during high-volume ticket drops.

---

## The Race Condition Problem

In viral ticket sales, thousands of users hit the purchase button at the exact same millisecond. 

A naive approach relies on application-level checks:
```java
// Vulnerable to race conditions
if (seat.getStatus() == SeatStatus.AVAILABLE) {
    seat.setStatus(SeatStatus.BOOKED);
    seatRepository.save(seat);
}
```
Under concurrent load, Thread A and Thread B read `AVAILABLE` simultaneously before either transaction commits. Both threads proceed to create a booking, causing a double-booked seat and corrupted business state.

---

## The Solution: Optimistic Concurrency Control

QuickTix uses **JPA/Hibernate Optimistic Locking** via an internal version tracking column on the [`Seat.java`](../server/src/main/java/com/asyncsourav/quicktix/entity/Seat.java) entity:

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

### How the Locking Mechanism Works Under the Hood

1. **Transactional Boundary**: The entire booking workflow in [`BookingService.java`](../server/src/main/java/com/asyncsourav/quicktix/service/BookingService.java) executes inside a Spring `@Transactional` context.
2. **Read Phase**: The application loads the selected seats along with their current version (e.g., `id = 10, version = 1`).
3. **Write Phase**: When updating the seat status, Hibernate issues a SQL query with a version predicate:
   ```sql
   UPDATE seats
   SET status = 'BOOKED', version = 2, updated_at = NOW()
   WHERE id = 10 AND version = 1;
   ```
4. **Collision Handling**:
   * **Winner Thread**: Executes first, updates the row, and increments `version` from `1` to `2`. Exactly 1 row is affected.
   * **Loser Thread**: Attempts to execute the update `WHERE id = 10 AND version = 1`. Because the version is now `2`, the update matches 0 rows.
5. **Atomic Batch Rollback**: Hibernate detects 0 updated rows and throws `ObjectOptimisticLockingFailureException`. Spring's transaction manager catches this and rolls back all seat reservations in that batch.
6. **HTTP 409 Conflict Translation**: [`GlobalExceptionHandler.java`](../server/src/main/java/com/asyncsourav/quicktix/exception/GlobalExceptionHandler.java) translates the exception into a clean, structured HTTP 409 response:
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

## Why Optimistic Locking vs Pessimistic Locking?

| Consideration | Pessimistic Locking (`SELECT FOR UPDATE`) | Optimistic Locking (`@Version`) |
| :--- | :--- | :--- |
| **Lock Mechanism** | Database row-level locks held for duration of transaction | No locks held; version checked at commit time |
| **Deadlock Risk** | High when reserving multiple seats in different order | Zero deadlock risk (no database locks held) |
| **Throughput** | Degrades rapidly under high concurrent read/write load | Maximum read throughput; non-conflicting writes run in parallel |
| **Connection Usage**| Keeps database connections busy waiting on locks | Releases connections quickly, maximizing connection pool efficiency |

---

## Concurrency Test Verification

Dispatched 10 parallel asynchronous booking requests targeting the exact same seat within the same millisecond:

* **Total Dispatched Requests**: 10
* **Successful Confirmations**: 1 (HTTP 201 Created)
* **Rejected Collisions**: 9 (HTTP 409 Conflict)
* **Database State**: Exactly 1 booking record created; seat status is `BOOKED`; `version` incremented from `1` to `2`.
