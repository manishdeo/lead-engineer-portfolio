package com.maplehub.ticketing.websocket.handler;

import com.maplehub.ticketing.common.event.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes booking events from Kafka and broadcasts
 * real-time seat updates to WebSocket subscribers.
 *
 * Clients subscribe to: /topic/seats/{showId}
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeatUpdateBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "booking-events", groupId = "websocket-service")
    public void handleBookingEvent(Object event) {
        if (event instanceof BookingEvent.SeatHeldEvent held) {
            broadcast(held.showId(), Map.of(
                    "type", "SEAT_HELD",
                    "seatIds", held.seatIds(),
                    "bookingId", held.bookingId()
            ));
        } else if (event instanceof BookingEvent.BookingConfirmedEvent confirmed) {
            broadcast(confirmed.showId(), Map.of(
                    "type", "SEAT_BOOKED",
                    "seatIds", confirmed.seatIds(),
                    "bookingId", confirmed.bookingId()
            ));
        } else if (event instanceof BookingEvent.SeatReleasedEvent released) {
            broadcast(released.showId(), Map.of(
                    "type", "SEAT_RELEASED",
                    "seatIds", released.seatIds()
            ));
        }
    }

    private void broadcast(Long showId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/seats/" + showId, payload);
        log.debug("Broadcast seat update: showId={}, payload={}", showId, payload);
    }
}
