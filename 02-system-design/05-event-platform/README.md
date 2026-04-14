# 🎪 Design Event Management Platform

---

## Requirements
- Create/manage events (conferences, concerts, meetups)
- Ticket types (VIP, General, Early Bird) with pricing tiers
- Registration and check-in
- Real-time attendee tracking
- Notifications (reminders, updates)
- Analytics dashboard

## Architecture
```
Client → API Gateway → Event Service → PostgreSQL
                    → Registration Service → PostgreSQL + Redis
                    → Notification Service → Kafka → Email/SMS/Push
                    → Analytics Service → ClickHouse
                    → Check-in Service → Redis (real-time counts)
```

## Key Design Decisions
- **Ticket inventory:** Redis atomic decrement for real-time availability
- **Check-in:** QR code + Redis for sub-second validation
- **Waitlist:** Priority queue in Redis sorted set
- **Analytics:** Event-driven pipeline → ClickHouse for real-time dashboards
- **Notifications:** Kafka for reliable delivery, scheduled reminders via cron

## Scaling Considerations
- Ticket sales spike at launch → auto-scale + queue-based processing
- Check-in spike at event start → Redis for fast lookups
- Multi-region for global events
