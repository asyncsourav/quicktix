

package com.asyncsourav.quicktix.service;



import com.asyncsourav.quicktix.dto.email.BookingConfirmationEmailDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;



/**
 * Service handling asynchronous email communications.
 * All email dispatches execute in background threads managed by mailTaskExecutor,
 * ensuring zero blocking on HTTP request threads or database transactions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {


    @Autowired(required = false)
    private JavaMailSender mailSender;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");


    /**
     * Asynchronously sends a booking confirmation email.
     * Executes on the dedicated 'mailTaskExecutor' thread pool.
     *
     * @param details Immutable details of the confirmed booking.
     */
    @Async("mailTaskExecutor")
    public void sendBookingConfirmation(BookingConfirmationEmailDetails details) {

        long startTime = System.currentTimeMillis();
        String currentThread = Thread.currentThread().getName();

        log.info(
                "[Async-Email] Starting booking confirmation email dispatch for Booking #{} to {} on thread [{}]",
                details.bookingId(), details.recipientEmail(), currentThread
        );

        String formattedDate = details.eventStartTime() != null 
                ? details.eventStartTime().format(DATE_TIME_FORMATTER) 
                : "TBD";

        String seatsString = details.seatLabels() != null && !details.seatLabels().isEmpty()
                ? String.join(", ", details.seatLabels()) 
                : "General Admission";

        String subject = "QuickTix Booking Confirmation - #" + details.bookingId() + " (" + details.eventTitle() + ")";
        String body = String.format("""
                Dear %s,

                Thank you for choosing QuickTix! Your booking has been confirmed.

                --------------------------------------------------
                BOOKING SUMMARY
                --------------------------------------------------
                Booking Reference: #%d
                Event:             %s
                Date & Time:       %s
                Venue:             %s
                Seats:             %s
                Total Paid:        $%s
                --------------------------------------------------

                Show this email or your booking reference at the venue gate for entry.

                Best regards,
                The QuickTix Team
                """,
                details.recipientName() != null ? details.recipientName() : "Valued Customer",
                details.bookingId(),
                details.eventTitle(),
                formattedDate,
                details.venueName(),
                seatsString,
                details.totalAmount() != null ? details.totalAmount().toPlainString() : "0.00"
        );

        boolean sentViaSmtp = false;
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(details.recipientEmail());
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                sentViaSmtp = true;
                log.info(
                        "[Async-Email] Delivered SMTP email for Booking #{} to {} in {} ms on thread [{}]",
                        details.bookingId(), details.recipientEmail(), System.currentTimeMillis() - startTime, currentThread
                );
            } catch (Exception ex) {
                log.warn(
                        "[Async-Email] SMTP server unreachable or rejected message for Booking #{} ({}). Falling back to console receipt. Error: {}",
                        details.bookingId(), details.recipientEmail(), ex.getMessage()
                );
            }
        }

        if (!sentViaSmtp) {
            log.info("""
                    [Async-Email Receipt] Booking #{}
                    To: {}
                    Subject: {}
                    Content:
                    {}
                    [Async-Email] Completed in-memory dispatch in {} ms on thread [{}] (zero client blocking)
                    """,
                    details.bookingId(),
                    details.recipientEmail(),
                    subject,
                    body.trim(),
                    System.currentTimeMillis() - startTime,
                    currentThread
            );
        }
    }




    /**
     * Asynchronously sends a booking cancellation confirmation email.
     *
     * @param recipientEmail User email address.
     * @param recipientName  User full name.
     * @param bookingId      Unique booking identifier.
     * @param eventTitle     Event title.
     * @param seatLabels     List of released seat labels.
     */
    @Async("mailTaskExecutor")
    public void sendBookingCancellation(
                String recipientEmail, 
                String recipientName, 
                Long bookingId, 
                String eventTitle, 
                List<String> seatLabels
        ) {

        long startTime = System.currentTimeMillis();
        String currentThread = Thread.currentThread().getName();

        log.info(
                "[Async-Email] Starting cancellation email dispatch for Booking #{} to {} on thread [{}]",
                bookingId, recipientEmail, currentThread
        );

        String seatsString = seatLabels != null && !seatLabels.isEmpty()
                ? String.join(", ", seatLabels)
                : "None";

        String subject = "QuickTix Booking Cancellation - #" + bookingId + " (" + eventTitle + ")";
        String body = String.format("""
                Dear %s,

                Your booking #%d for '%s' has been successfully cancelled.
                Released Seats: %s

                If a refund is applicable, it will be processed to your original payment method.

                Best regards,
                The QuickTix Team
                """,
                recipientName != null ? recipientName : "Valued Customer",
                bookingId,
                eventTitle,
                seatsString
        );

        boolean sentViaSmtp = false;
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipientEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                sentViaSmtp = true;
                log.info(
                        "[Async-Email] Delivered cancellation SMTP email for Booking #{} to {} in {} ms on thread [{}]",
                        bookingId, recipientEmail, System.currentTimeMillis() - startTime, currentThread
                );
            } catch (Exception ex) {
                log.warn(
                        "[Async-Email] SMTP server unreachable for cancellation email Booking #{} ({}). Error: {}",
                        bookingId, recipientEmail, ex.getMessage()
                );
            }
        }

        if (!sentViaSmtp) {
            log.info("""
                    [Async-Email Cancellation Receipt] Booking #{}
                    To: {}
                    Subject: {}
                    Content:
                    {}
                    [Async-Email] Cancellation notice completed in {} ms on thread [{}]
                    """,
                    bookingId,
                    recipientEmail,
                    subject,
                    body.trim(),
                    System.currentTimeMillis() - startTime,
                    currentThread
            );
        }
    }
}
