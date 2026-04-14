package com.maplehub.ticketing.notification.service;

import com.maplehub.ticketing.common.event.BookingEvent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BookingNotificationService {

    @KafkaListener(topics = "booking-events", groupId = "notification-service")
    public void handleBookingEvent(Object event) {
        if (event instanceof BookingConfirmedEvent confirmed) {
            log.info("Sending booking confirmation: bookingId={}, customer={}",
                    confirmed.bookingId(), confirmed.customerId());
            // Send email/SMS confirmation with ticket details
        } else if (event instanceof BookingCancelledEvent cancelled) {
            log.info("Sending cancellation notice: bookingId={}, reason={}",
                    cancelled.bookingId(), cancelled.reason());
            // Send cancellation email/SMS
        }
    }
}
