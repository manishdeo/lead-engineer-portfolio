const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export interface WindowedAggregation {
  tenantId: string;
  eventType: string;
  windowStart: string;
  windowEnd: string;
  count: number;
  uniqueUsers: number;
}

export interface DashboardQuery {
  tenantId: string;
  eventType?: string;
  from?: string;
  to?: string;
  granularity?: string;
}

export async function fetchDashboard(query: DashboardQuery): Promise<WindowedAggregation[]> {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([k, v]) => { if (v) params.set(k, v); });
  const res = await fetch(`${API_URL}/api/dashboard?${params}`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch dashboard data');
  return res.json();
}

export async function fetchTopEvents(tenantId: string, limit = 10) {
  const res = await fetch(`${API_URL}/api/dashboard/top-events?tenantId=${tenantId}&limit=${limit}`, {
    cache: 'no-store',
  });
  if (!res.ok) throw new Error('Failed to fetch top events');
  return res.json();
}

export async function ingestEvent(event: {
  tenantId: string;
  eventType: string;
  userId: string;
  properties?: Record<string, string>;
}) {
  await fetch(`${API_URL}/api/events`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(event),
  });
}
