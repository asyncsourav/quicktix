

package com.asyncsourav.quicktix.config;



import com.asyncsourav.quicktix.entity.*;
import com.asyncsourav.quicktix.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;




/**
 * Seeds demo accounts, venues, and events on application startup.
 * Provides immediate 1-click playability for Attendee, Organizer, and Admin roles.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {



    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;



    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already initialized. Skipping seed data.");
            return;
        }

        log.info("Seeding QuickTix demo users, venues, and events...");

        // 1. Seed Demo Users (Password: password123)
        String defaultPasswordHash = passwordEncoder.encode("password123");

        User admin = User.builder()
                .name("Alex (Admin)")
                .email("admin@quicktix.com")
                .passwordHash(defaultPasswordHash)
                .role(Role.ADMIN)
                .build();

        User organizer = User.builder()
                .name("Sarah (Organizer)")
                .email("organizer@quicktix.com")
                .passwordHash(defaultPasswordHash)
                .role(Role.ORGANIZER)
                .build();

        User customer = User.builder()
                .name("Sourav (Attendee)")
                .email("user@quicktix.com")
                .passwordHash(defaultPasswordHash)
                .role(Role.USER)
                .build();

        userRepository.saveAll(List.of(admin, organizer, customer));
        log.info("Created 3 demo accounts (admin@quicktix.com, organizer@quicktix.com, user@quicktix.com)");

        // 2. Seed Demo Venues
        Venue arena = Venue.builder()
                .name("Madison Square Garden")
                .address("4 Pennsylvania Plaza, New York, NY")
                .totalCapacity(50)
                .organizer(organizer)
                .build();

        Venue amphitheater = Venue.builder()
                .name("Red Rocks Amphitheatre")
                .address("18300 W Alameda Pkwy, Morrison, CO")
                .totalCapacity(40)
                .organizer(organizer)
                .build();

        Venue techCenter = Venue.builder()
                .name("Moscone Center")
                .address("747 Howard St, San Francisco, CA")
                .totalCapacity(30)
                .organizer(organizer)
                .build();

        venueRepository.saveAll(List.of(arena, amphitheater, techCenter));

        // 3. Seed Demo Events
        Event concert = Event.builder()
                .title("Coldplay: Music of the Spheres World Tour")
                .description("Experience an electrifying night of stadium rock, lights, and world-class visual effects live in concert.")
                .category("Concert")
                .basePrice(new BigDecimal("89.99"))
                .startTime(LocalDateTime.now().plusDays(14).withHour(20).withMinute(0))
                .venue(arena)
                .organizer(organizer)
                .build();

        Event sports = Event.builder()
                .title("NBA Finals: Game 7 Championship")
                .description("The ultimate showdown for the championship trophy. Witness the highest-stakes basketball battle live courtside.")
                .category("Sports")
                .basePrice(new BigDecimal("120.00"))
                .startTime(LocalDateTime.now().plusDays(21).withHour(19).withMinute(30))
                .venue(arena)
                .organizer(organizer)
                .build();

        Event orchestra = Event.builder()
                .title("Hans Zimmer Live: Symphonic Cinema")
                .description("Iconic movie themes from Interstellar, Inception, and Gladiator performed live with a full 80-piece orchestra.")
                .category("Concert")
                .basePrice(new BigDecimal("65.50"))
                .startTime(LocalDateTime.now().plusDays(7).withHour(19).withMinute(0))
                .venue(amphitheater)
                .organizer(organizer)
                .build();

        Event techConf = Event.builder()
                .title("Global AI & Distributed Systems Summit 2026")
                .description("The premier engineering conference for high-concurrency systems, AI agents, and next-gen infrastructure.")
                .category("Conference")
                .basePrice(new BigDecimal("199.00"))
                .startTime(LocalDateTime.now().plusDays(30).withHour(9).withMinute(0))
                .venue(techCenter)
                .organizer(organizer)
                .build();

        List<Event> events = List.of(concert, sports, orchestra, techConf);
        eventRepository.saveAll(events);

        // 4. Generate Seats for each Event
        for (Event event : events) {
            int capacity = event.getVenue().getTotalCapacity();
            List<Seat> seats = new ArrayList<>(capacity);
            int seatsPerRow = 10;

            for (int i = 0; i < capacity; i++) {
                char rowLetter = (char) ('A' + (i / seatsPerRow));
                int seatNum = (i % seatsPerRow) + 1;
                String seatLabel = String.format("%c%d", rowLetter, seatNum);

                Seat seat = Seat.builder()
                        .event(event)
                        .seatLabel(seatLabel)
                        .price(event.getBasePrice())
                        .status(SeatStatus.AVAILABLE)
                        .build();

                seats.add(seat);
            }
            seatRepository.saveAll(seats);
        }

        // 5. Seed a Sample Confirmed Booking for the Customer
        List<Seat> coldplaySeats = seatRepository.findByEventIdOrderBySeatLabelAsc(concert.getId());
        if (coldplaySeats.size() >= 2) {
            Seat s1 = coldplaySeats.get(0);
            Seat s2 = coldplaySeats.get(1);

            BigDecimal totalAmount = s1.getPrice().add(s2.getPrice());

            Booking sampleBooking = Booking.builder()
                    .user(customer)
                    .event(concert)
                    .totalAmount(totalAmount)
                    .status(BookingStatus.CONFIRMED)
                    .seats(new ArrayList<>())
                    .build();

            Booking savedBooking = bookingRepository.save(sampleBooking);

            s1.setStatus(SeatStatus.BOOKED);
            s1.setBooking(savedBooking);
            s2.setStatus(SeatStatus.BOOKED);
            s2.setBooking(savedBooking);

            savedBooking.getSeats().add(s1);
            savedBooking.getSeats().add(s2);

            seatRepository.saveAll(List.of(s1, s2));
        }

        log.info("QuickTix demo dataset seeded successfully! (3 users, 3 venues, 4 events, 120+ seats, 1 sample booking)");
    }
}
