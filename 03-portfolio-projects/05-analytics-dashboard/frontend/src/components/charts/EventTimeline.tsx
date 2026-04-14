'use client';

import { useMemo } from 'react';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import type { WindowedAggregation } from '@/lib/api';

interface Props {
  data: WindowedAggregation[];
}

export function EventTimeline({ data }: Props) {
  const chartData = useMemo(
    () =>
      data.map((d) => ({
        time: new Date(d.windowStart).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        count: d.count,
        type: d.eventType,
      })),
    [data]
  );

  if (chartData.length === 0) {
    return <p className="text-gray-500 text-center py-12">Waiting for events...</p>;
  }

  return (
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart data={chartData}>
        <defs>
          <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
            <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
        <XAxis dataKey="time" stroke="#9ca3af" fontSize={12} />
        <YAxis stroke="#9ca3af" fontSize={12} />
        <Tooltip
          contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
          labelStyle={{ color: '#9ca3af' }}
        />
        <Area type="monotone" dataKey="count" stroke="#3b82f6" fill="url(#colorCount)" strokeWidth={2} />
      </AreaChart>
    </ResponsiveContainer>
  );
}
