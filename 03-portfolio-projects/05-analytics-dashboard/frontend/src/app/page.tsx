import { fetchDashboard, fetchTopEvents } from '@/lib/api';
import { DashboardShell } from '@/components/layout/DashboardShell';

/**
 * Server Component — fetches initial dashboard data at request time.
 * No JS shipped to client for this component.
 *
 * Interview: Why Server Components for analytics?
 * - Initial data fetched server-side → faster FCP, no loading spinner
 * - Reduces client JS bundle (no fetch logic in browser)
 * - Client Components only for interactive charts + WebSocket
 */
export default async function HomePage() {
  const tenantId = 'demo';

  const [initialData, topEvents] = await Promise.all([
    fetchDashboard({ tenantId, granularity: '1m' }).catch(() => []),
    fetchTopEvents(tenantId, 5).catch(() => []),
  ]);

  return (
    <main className="max-w-7xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-2">📊 Real-time Analytics</h1>
      <p className="text-gray-400 mb-8">
        Kafka Streams → ClickHouse → WebSocket → React
      </p>
      <DashboardShell tenantId={tenantId} initialData={initialData} topEvents={topEvents} />
    </main>
  );
}
