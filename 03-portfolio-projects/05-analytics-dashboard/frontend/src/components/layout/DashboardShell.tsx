'use client';

import { useMemo } from 'react';
import { useWebSocket } from '@/hooks/useWebSocket';
import { EventTimeline } from '@/components/charts/EventTimeline';
import { TopEventsChart } from '@/components/charts/TopEventsChart';
import { MetricCards } from '@/components/charts/MetricCards';
import type { WindowedAggregation } from '@/lib/api';

interface Props {
  tenantId: string;
  initialData: WindowedAggregation[];
  topEvents: Array<{ event_type: string; total: number }>;
}

/**
 * Client Component shell — merges server-rendered initial data with
 * live WebSocket stream. Charts re-render only when data changes.
 */
export function DashboardShell({ tenantId, initialData, topEvents }: Props) {
  const { data: liveData, connected } = useWebSocket(tenantId);

  const timelineData = useMemo(
    () => [...initialData, ...liveData],
    [initialData, liveData]
  );

  const totalEvents = useMemo(
    () => timelineData.reduce((sum, d) => sum + d.count, 0),
    [timelineData]
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3 mb-4">
        <span className={`h-3 w-3 rounded-full ${connected ? 'bg-green-500' : 'bg-red-500'}`} />
        <span className="text-sm text-gray-400">
          {connected ? 'Live' : 'Connecting...'}
        </span>
      </div>

      <MetricCards
        totalEvents={totalEvents}
        liveUpdates={liveData.length}
        topEventType={topEvents[0]?.event_type ?? '-'}
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-gray-900 rounded-xl p-6">
          <h2 className="text-lg font-semibold mb-4">Event Timeline</h2>
          <EventTimeline data={timelineData} />
        </div>
        <div className="bg-gray-900 rounded-xl p-6">
          <h2 className="text-lg font-semibold mb-4">Top Events (1h)</h2>
          <TopEventsChart data={topEvents} />
        </div>
      </div>
    </div>
  );
}
