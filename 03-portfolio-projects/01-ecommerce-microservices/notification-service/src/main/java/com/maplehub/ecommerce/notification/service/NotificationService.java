package com.maplehub.ecommerce.notification.service;

import com.maplehub.ecommerce.common.event.OrderEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @KafkaListener(topics = "notification-events", groupId = "notification-service")
    public void handleNotificationEvent(Object event) {
        if (event instanceof OrderConfirmedEvent confirmed) {
            sendOrderConfirmation(confirmed);
        } else if (event instanceof OrderCancelledEvent cancelled) {
            sendOrderCancellation(cancelled);
        }
    }

    private void sendOrderConfirmation(OrderConfirmedEvent event) {
        log.info("Sending order confirmation: orderId={}, customer={}", event.getOrderId(), event.getCustomerId());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getCustomerId()); // In real app, resolve email from customer ID
            message.setSubject("Order Confirmed - #" + event.getOrderId());
            message.setText("Your order #" + event.getOrderId() + " has been confirmed.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send confirmation email for order: {}", event.getOrderId(), e);
        }
    }

    private void sendOrderCancellation(OrderCancelledEvent event) {
        log.info("Sending order cancellation: orderId={}, reason={}", event.getOrderId(), event.getReason());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getCustomerId());
            message.setSubject("Order Cancelled - #" + event.getOrderId());
            message.setText("Your order #" + event.getOrderId() + " has been cancelled. Reason: " + event.getReason());
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send cancellation email for order: {}", event.getOrderId(), e);
        }
    }
}
