

package com.asyncsourav.quicktix.entity;



import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * CONCURRENCY & OPTIMISTIC LOCKING:
 * The `@Version` field (`version`) is the core mechanism that prevents double-booking race conditions.
 * When multiple users attempt to hold/book the same seat at the same millisecond, Hibernate compares
 * the version counter. The first update increments the version; subsequent concurrent updates detect
 * the mismatch and throw an `OptimisticLockException` (which triggers a rollback and clean failure message).
 */

@Entity
@Table(name = "seats",
        uniqueConstraints = {
                // combination of event_id + seat_label must be unique
                @UniqueConstraint(name = "uk_event_seat_label", columnNames = {"event_id", "seat_label"})
        },
        indexes = {
                //creates a database index to fasten the response
                @Index(name = "idx_seats_event_status", columnList = "event_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"event", "booking"})
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Event is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @NotBlank(message = "Seat label is required")
    @Size(max = 20, message = "Seat label must not exceed 20 characters")
    @Column(name = "seat_label", nullable = false, length = 20)
    private String seatLabel;

    @NotNull(message = "Seat price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Seat price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Seat status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    /**
     * Optimistic Locking Version field.
     * Managed automatically by Hibernate on each UPDATE statement.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


/**
 * what does the version and seatStatus does ??
 * why use both ??
 *                                  [ DATABASE ]
 *                            Seat A1: status = AVAILABLE
 *                                     version = 0
 *                                          │
 *                  ┌───────────────────────┴───────────────────────┐
 *                  ▼                                               ▼
 *          [ SOURAV'S THREAD ]                             [ ALEX'S THREAD ]
 *    1. Reads Seat A1 (version = 0)                  1. Reads Seat A1 (version = 0)
 *    2. Changes status to 'HELD'                     2. Changes status to 'HELD'
 *    3. Sends SQL UPDATE to DB:                      3. Sends SQL UPDATE to DB (1ms later):
 *       UPDATE seats                                    - UPDATE seats
 *       SET status = 'HELD', version = 1                SET status = 'HELD', version = 1
 *       WHERE id = 10 AND version = 0;                  WHERE id = 10 AND version = 0;
 *                  │                                               │
 *                  ▼                                               ▼
 *       MATCH! (DB version was 0)                    NO MATCH! (DB version is now 1)
 *       -> Rows updated: 1                              -> Rows updated: 0
 *       -> DB version becomes 1                         -> Hibernate throws OptimisticLockException!
 *       -> SOURAV GETS THE SEAT!                        -> ALEX'S TRANSACTION ROLLS BACK!
 */

