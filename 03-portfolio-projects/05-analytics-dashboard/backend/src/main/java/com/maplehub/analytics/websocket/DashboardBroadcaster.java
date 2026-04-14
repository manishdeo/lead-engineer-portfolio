package com.maplehub.analytics.websocket;

import com.maplehub.analytics.model.WindowedAggregation;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Broadcasts real-time aggregation results to WebSocket subscribers.
 * Clients subscribe to /topic/dashboard/{tenantId} for tenant-scoped updates.
 *
 * Interview: Why STOMP over raw WebSocket?
 * - Built-in pub/sub with topic routing
 * - Message framing and content negotiation
 * - SockJS fallback for environments blocking WebSocket
 */
@Component
public class DashboardBroadcaster {

    private final SimpMessagingTemplate messaging;

    public DashboardBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void broadcast(String tenantId, WindowedAggregation aggregation) {
        messaging.convertAndSend("/topic/dashboard/" + tenantId, aggregation);
    }
}
