# Real-Time Seat Synchronization Architecture (WebSocket / STOMP)

**Objective:** Sub-millisecond full-duplex seat status broadcast (`AVAILABLE`, `HELD`, `BOOKED`) via Spring STOMP broker to eliminate manual seat map page refreshes.

---

## STOMP vs Socket.io Conceptual Mapping

| Architectural Concept | Node.js / Socket.io | Spring Boot / STOMP | QuickTix Implementation |
| :--- | :--- | :--- | :--- |
| **Transport Handshake** | `io.connect('http://localhost:3000')` | `/ws` with SockJS fallback | `registry.addEndpoint("/ws").withSockJS()` in [`WebSocketConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/WebSocketConfig.java) |
| **Rooms / Channels** | `socket.join('event:1')` | Destination Topic | `/topic/event.{eventId}` via simple in-memory broker |
| **Server Broadcast** | `io.to('event:1').emit('seat', payload)` | `SimpMessagingTemplate` | `messagingTemplate.convertAndSend(destination, payload)` in [`SeatBroadcastService.java`](../server/src/main/java/com/asyncsourav/quicktix/service/SeatBroadcastService.java) |
| **Client Listener** | `socket.on('seat', handler)` | `stompClient.subscribe(topic, handler)` | `@stomp/stompjs` client in [`EventDetailPage.jsx`](../client/src/pages/EventDetailPage.jsx) |

---

## Broker & Endpoint Configuration ([`WebSocketConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/WebSocketConfig.java))

```java
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final CorsProperties corsProperties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = corsProperties.getAllowedOrigins().toArray(new String[0]);
        registry.addEndpoint("/ws").setAllowedOriginPatterns(origins).withSockJS();
        registry.addEndpoint("/ws").setAllowedOriginPatterns(origins);
    }
}
```

* Handshake endpoint `/ws/**` is explicitly permitted in [`SecurityConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/SecurityConfig.java).
* Reverse proxy with `ws: true` configured in [`vite.config.js`](../client/vite.config.js).

---

## Broadcast Payload Schema ([`SeatStatusUpdate.java`](../server/src/main/java/com/asyncsourav/quicktix/dto/response/SeatStatusUpdate.java))

Dispatched to destination topic `/topic/event.{eventId}`:

```json
{
  "seatId": 10,
  "seatLabel": "A1",
  "status": "BOOKED",
  "eventId": 1
}
```

---

## Broadcast Triggers in [`BookingService.java`](../server/src/main/java/com/asyncsourav/quicktix/service/BookingService.java)

1. **Ticket Booking (`createBooking`):**
   * Seats transition from `AVAILABLE` to `BOOKED` and save to PostgreSQL.
   * `SeatBroadcastService.broadcastSeatStatusBatch(eventId, updates)` broadcasts `BOOKED` status to all attendees currently viewing that event.
2. **Ticket Cancellation (`cancelBooking`):**
   * Seats transition from `BOOKED` back to `AVAILABLE`.
   * Broadcasts `AVAILABLE` status, immediately making released seats selectable again for competing users.

---

## Frontend Subscription ([`EventDetailPage.jsx`](../client/src/pages/EventDetailPage.jsx))

```javascript
stompClient.subscribe(`/topic/event.${id}`, (message) => {
  const updates = Array.isArray(JSON.parse(message.body)) ? JSON.parse(message.body) : [JSON.parse(message.body)];
  
  // 1. Update seat map visual colors dynamically without page refresh
  setSeats((prev) => {
    const map = new Map(updates.map((u) => [u.seatId, u.status]));
    return prev.map((s) => map.has(s.id) ? { ...s, status: map.get(s.id) } : s);
  });

  // 2. Automatically deselect if another attendee secured the seat
  setSelectedSeatIds((prev) =>
    prev.filter((id) => !updates.some((u) => u.seatId === id && u.status !== 'AVAILABLE'))
  );
});
```

* Displays a **Live Seat Sync** badge reflecting active STOMP connection health.
