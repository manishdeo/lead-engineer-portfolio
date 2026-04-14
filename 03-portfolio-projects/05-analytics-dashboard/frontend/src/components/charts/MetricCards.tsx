'use client';

interface Props {
  totalEvents: number;
  liveUpdates: number;
  topEventType: string;
}

export function MetricCards({ totalEvents, liveUpdates, topEventType }: Props) {
  const cards = [
    { label: 'Total Events', value: totalEvents.toLocaleString(), color: 'text-blue-400' },
    { label: 'Live Updates', value: liveUpdates.toString(), color: 'text-green-400' },
    { label: 'Top Event', value: topEventType, color: 'text-purple-400' },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
      {cards.map((card) => (
        <div key={card.label} className="bg-gray-900 rounded-xl p-5">
          <p className="text-sm text-gray-400">{card.label}</p>
          <p className={`text-2xl font-bold mt-1 ${card.color}`}>{card.value}</p>
        </div>
      ))}
    </div>
  );
}
