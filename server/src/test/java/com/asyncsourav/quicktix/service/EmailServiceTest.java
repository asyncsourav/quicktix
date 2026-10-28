

package com.asyncsourav.quicktix.service;



import com.asyncsourav.quicktix.dto.email.BookingConfirmationEmailDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;



@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;



    @Test
    @DisplayName("sendBookingConfirmation: Builds and sends message via mailSender")
    void sendBookingConfirmation_BuildsAndSends() {

        EmailService emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);

        BookingConfirmationEmailDetails details = new BookingConfirmationEmailDetails(
                "user@quicktix.com",
                "Alice",
                101L,
                "Coldplay Live",
                LocalDateTime.now().plusDays(2),
                "O2 Arena",
                List.of("A1"),
                new BigDecimal("85.00")
        );

        emailService.sendBookingConfirmation(details);

        ArgumentCaptor<SimpleMailMessage> msgCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msgCaptor.capture());

        SimpleMailMessage sent = msgCaptor.getValue();
        assertThat(sent.getTo()).containsExactly("user@quicktix.com");
        assertThat(sent.getSubject()).contains("Coldplay Live");
    }



    @Test
    @DisplayName("sendBookingConfirmation: Mail sender failure does not propagate exception")
    void sendBookingConfirmation_FailureDoesNotPropagate() {

        EmailService emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
        doThrow(new MailSendException("SMTP timeout")).when(mailSender).send(any(SimpleMailMessage.class));

        BookingConfirmationEmailDetails details = new BookingConfirmationEmailDetails(
                "user@quicktix.com", "Alice", 101L, "Coldplay Live", null, "O2 Arena", List.of(), null
        );

        assertThatNoException().isThrownBy(() -> emailService.sendBookingConfirmation(details));
    }
}

