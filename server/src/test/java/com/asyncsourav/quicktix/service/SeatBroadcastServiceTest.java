


package com.asyncsourav.quicktix.service;




import com.asyncsourav.quicktix.dto.response.SeatStatusUpdate;
import com.asyncsourav.quicktix.entity.SeatStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
class SeatBroadcastServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SeatBroadcastService seatBroadcastService;



    @Test
    @DisplayName("broadcastSeatStatus: Sends a single update to the event topic")
    void broadcastSeatStatus_ShouldSendSingleMessage() {

        seatBroadcastService.broadcastSeatStatus(7L, 21L, "A5", SeatStatus.BOOKED);
        verify(messagingTemplate).convertAndSend(eq("/topic/event.7"), any(SeatStatusUpdate.class));
    }



    @Test
    @DisplayName("broadcastSeatStatusBatch: Sends each update in the batch")
    void broadcastSeatStatusBatch_ShouldSendBatchMessages() {

        List<SeatStatusUpdate> updates = List.of(
                new SeatStatusUpdate(21L, "A5", SeatStatus.BOOKED, 7L),
                new SeatStatusUpdate(22L, "A6", SeatStatus.AVAILABLE, 7L)
        );

        seatBroadcastService.broadcastSeatStatusBatch(7L, updates);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/event.7"), any(SeatStatusUpdate.class));
    }




    @Test
    @DisplayName("broadcastSeatStatusBatch: Ignores null or empty batch")
    void broadcastSeatStatusBatch_WhenUpdatesAreEmpty_ShouldReturnWithoutSending() {
        
        seatBroadcastService.broadcastSeatStatusBatch(7L, null);
        seatBroadcastService.broadcastSeatStatusBatch(7L, List.of());

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(SeatStatusUpdate.class));
    }
}
