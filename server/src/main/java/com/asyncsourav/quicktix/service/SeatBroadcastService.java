

package com.asyncsourav.quicktix.service;



import com.asyncsourav.quicktix.dto.response.SeatStatusUpdate;
import com.asyncsourav.quicktix.entity.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;



/**
 * Service responsible for broadcasting live seat status transitions (AVAILABLE, HELD, BOOKED)
 * to connected frontend clients via Spring WebSocket / STOMP broker.
 * Destination topic: /topic/event.{eventId}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatBroadcastService {


    private final SimpMessagingTemplate messagingTemplate;


    /**
     * Broadcasts a single seat status update to all clients viewing the specified event.
     */
    public void broadcastSeatStatus(Long eventId, Long seatId, String seatLabel, SeatStatus status) {
        String destination = "/topic/event." + eventId;
        SeatStatusUpdate update = new SeatStatusUpdate(seatId, seatLabel, status, eventId);

        try {
            log.info(
                    "Broadcasting seat status update to {}: seatId={}, label={}, status={}",
                    destination, 
                    seatId, 
                    seatLabel, 
                    status
            );
            messagingTemplate.convertAndSend(destination, update);

        } catch (Exception e) {
            log.error(
                    "Failed to broadcast seat status update to {}: {}", 
                    destination, e.getMessage(), e
            );
        }
    }



    /**
     * Broadcasts multiple seat status updates (e.g. multi-seat booking confirmation or cancellation).
     */
    public void broadcastSeatStatusBatch(Long eventId, List<SeatStatusUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        String destination = "/topic/event." + eventId;
        try {
            log.info(
                    "Broadcasting batch of {} seat updates to {}", 
                    updates.size(), 
                    destination
            );
            for (SeatStatusUpdate update : updates) {
                messagingTemplate.convertAndSend(destination, update);
            }
        } catch (Exception e) {
            log.error(
                    "Failed to broadcast batch seat updates to {}: {}", 
                    destination, e.getMessage(), e
            );
        }
    }
}
