'use client';

import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

interface Props {
  data: Array<{ event_type: string; total: number }>;
}

export function TopEventsChart({ data }: Props) {
  if (data.length === 0) {
    return <p className="text-gray-500 text-center py-12">No data yet</p>;
  }

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data} layout="vertical">
        <XAxis type="number" stroke="#9ca3af" fontSize={12} />
        <YAxis type="category" dataKey="event_type" stroke="#9ca3af" fontSize={12} width={100} />
        <Tooltip
          contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
        />
        <Bar dataKey="total" fill="#8b5cf6" radius={[0, 4, 4, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
