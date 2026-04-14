'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import type { WindowedAggregation } from '@/lib/api';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080/ws/analytics';
const MAX_BUFFER = 100;

/**
 * Custom hook for WebSocket connection to analytics backend.
 * Subscribes to tenant-scoped aggregation updates.
 *
 * Interview: Why buffer with drop-oldest?
 * - Prevents memory leak if dashboard tab is backgrounded
 * - Keeps most recent data for chart rendering
 * - Client-side backpressure without blocking server
 */
export function useWebSocket(tenantId: string) {
  const [data, setData] = useState<WindowedAggregation[]>([]);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  const clear = useCallback(() => setData([]), []);

  useEffect(() => {
    const client = new Client({
      brokerURL: undefined,
      webSocketFactory: () => new WebSocket(WS_URL.replace('http', 'ws')),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/dashboard/${tenantId}`, (message) => {
          const aggregation: WindowedAggregation = JSON.parse(message.body);
          setData((prev) => {
            const next = [...prev, aggregation];
            return next.length > MAX_BUFFER ? next.slice(-MAX_BUFFER) : next;
          });
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => console.error('STOMP error:', frame.headers['message']),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [tenantId]);

  return { data, connected, clear };
}
