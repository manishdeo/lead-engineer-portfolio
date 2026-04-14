# 🚗 Design Uber/Ride-Sharing System

> Complete system design for a ride-sharing platform like Uber/Lyft

---

## 1. Requirements Clarification

### Functional Requirements

✅ **Core Features:**
- Riders can request rides
- Drivers can accept/reject ride requests
- Real-time location tracking
- ETA calculation
- Fare calculation
- Payment processing
- Ride history
- Ratings & reviews

✅ **Additional Features:**
- Surge pricing
- Multiple ride types (UberX, UberXL, UberPool)
- Driver earnings tracking
- Ride scheduling
- Promo codes

### Non-Functional Requirements

- **Availability:** 99.99% uptime
- **Latency:** <200ms for matching, <1s for location updates
- **Scale:** 100M users, 1M drivers, 10M rides/day
- **Consistency:** Eventual consistency acceptable for most operations
- **Security:** PCI-DSS compliant for payments

---

## 2. Capacity Estimation

### Traffic Estimates

```
Daily Active Users: 10M riders, 1M drivers
Rides per day: 10M
Peak hours (8-10 AM, 5-8 PM): 3x average

Average RPS: 10M / 86400 ≈ 116 rides/sec
Peak RPS: 350 rides/sec

Location updates:
- 1M active drivers * 1 update/4 sec = 250K updates/sec
- 10M active riders * 1 update/5 sec = 2M updates/sec
Total location updates: 2.25M/sec
```

### Storage Estimates

```
Per ride data: 1KB
Rides per day: 10M
Daily storage: 10M * 1KB = 10GB
Yearly storage: 10GB * 365 = 3.65TB
With 5 years retention: 18.25TB
With replication (3x): 55TB

Location data (hot data, 24 hours):
2.25M updates/sec * 100 bytes * 86400 sec = 19.44TB/day
```

### Bandwidth

```
Location updates: 2.25M/sec * 100 bytes = 225 MB/s
Ride requests: 350/sec * 10KB = 3.5 MB/s
Total: ~230 MB/s
```

---

## 3. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Clients                              │
│              (Rider App)    (Driver App)                     │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    CDN (Static Assets)                       │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│              Load Balancer (AWS ALB/NLB)                     │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                  API Gateway                                 │
│         (Authentication, Rate Limiting, Routing)             │
└─────┬──────────┬──────────┬──────────┬──────────┬──────────┘
      │          │          │          │          │
┌─────▼────┐ ┌──▼────┐ ┌───▼────┐ ┌───▼────┐ ┌──▼────────┐
│  User    │ │ Ride  │ │Location│ │Payment │ │Notification│
│ Service  │ │Service│ │Service │ │Service │ │  Service   │
└─────┬────┘ └──┬────┘ └───┬────┘ └───┬────┘ └──┬─────────┘
      │         │          │          │         │
┌─────▼─────────▼──────────▼──────────▼─────────▼──────────┐
│                    Message Queue (Kafka)                   │
└────────────────────────────────────────────────────────────┘
      │         │          │          │         │
┌─────▼────┐ ┌──▼────┐ ┌───▼────┐ ┌───▼────┐ ┌──▼─────────┐
│PostgreSQL│ │ Redis │ │Cassandra│ │Stripe │ │   SNS/FCM  │
│(User/Ride│ │(Cache)│ │(Location│ │  API  │ │(Push Notif)│
│  Data)   │ │       │ │  Data)  │ │       │ │            │
└──────────┘ └───────┘ └─────────┘ └───────┘ └────────────┘
```

---

## 4. Core Components Design

### 4.1 Location Service

**Geospatial Indexing with QuadTree:**

```java
// QuadTree for efficient driver search
public class QuadTree {
    private static final int MAX_CAPACITY = 50;
    private static final int MAX_DEPTH = 6;
    
    private Boundary boundary;
    private List<Driver> drivers;
    private QuadTree[] children;
    private int depth;
    
    public List<Driver> findNearbyDrivers(Location location, double radiusKm) {
        List<Driver> result = new ArrayList<>();
        searchInRadius(location, radiusKm, result);
        return result;
    }
    
    private void searchInRadius(Location center, double radius, List<Driver> result) {
        if (!boundary.intersectsCircle(center, radius)) {
            return;
        }
        
        // Check drivers in current node
        for (Driver driver : drivers) {
            if (driver.getLocation().distanceTo(center) <= radius) {
                result.add(driver);
            }
        }
        
        // Search children
        if (children != null) {
            for (QuadTree child : children) {
                child.searchInRadius(center, radius, result);
            }
        }
    }
    
    public void updateDriverLocation(Driver driver, Location newLocation) {
        // Remove from old position
        remove(driver);
        // Insert at new position
        insert(driver, newLocation);
    }
}

// Alternative: Geohash
public class GeohashService {
    
    public String encode(double lat, double lon, int precision) {
        // Encode lat/lon to geohash string
        // Precision: 6 chars ≈ 1.2km, 7 chars ≈ 150m
        return Geohash.encode(lat, lon, precision);
    }
    
    public List<Driver> findNearbyDrivers(Location location, double radiusKm) {
        String centerHash = encode(location.getLat(), location.getLon(), 6);
        List<String> neighbors = getNeighbors(centerHash);
        
        // Query Redis for all geohashes
        List<Driver> candidates = new ArrayList<>();
        for (String hash : neighbors) {
            candidates.addAll(redisTemplate.opsForGeo()
                .radius("drivers", new Circle(location, radiusKm)));
        }
        
        return candidates;
    }
}
```

**Location Update Service:**

```java
@Service
public class LocationService {
    
    private final RedisTemplate<String, Driver> redisTemplate;
    private final KafkaTemplate<String, LocationUpdate> kafkaTemplate;
    
    // Real-time location update via WebSocket
    @MessageMapping("/location/update")
    public void updateLocation(LocationUpdate update) {
        // Update Redis with geospatial data
        redisTemplate.opsForGeo().add(
            "drivers:active",
            new Point(update.getLongitude(), update.getLatitude()),
            update.getDriverId()
        );
        
        // Publish to Kafka for persistence
        kafkaTemplate.send("location-updates", update);
        
        // Update QuadTree for fast matching
        quadTreeService.updateDriverLocation(
            update.getDriverId(),
            new Location(update.getLatitude(), update.getLongitude())
        );
    }
    
    // Find nearby drivers
    public List<Driver> findNearbyDrivers(Location location, double radiusKm) {
        // Query Redis Geo
        GeoResults<GeoLocation<Driver>> results = redisTemplate.opsForGeo()
            .radius("drivers:active",
                new Circle(new Point(location.getLon(), location.getLat()), 
                          new Distance(radiusKm, Metrics.KILOMETERS)),
                GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .includeDistance()
                    .sortAscending()
                    .limit(20));
        
        return results.getContent().stream()
            .map(result -> result.getContent().getName())
            .collect(Collectors.toList());
    }
}
```

---

### 4.2 Ride Matching Service

```java
@Service
public class RideMatchingService {
    
    private final LocationService locationService;
    private final DriverService driverService;
    private final NotificationService notificationService;
    private final RedisTemplate<String, String> redisTemplate;
    
    public CompletableFuture<RideMatch> matchRide(RideRequest request) {
        // Find nearby available drivers
        List<Driver> nearbyDrivers = locationService.findNearbyDrivers(
            request.getPickupLocation(),
            5.0 // 5km radius
        );
        
        // Filter available drivers
        List<Driver> availableDrivers = nearbyDrivers.stream()
            .filter(Driver::isAvailable)
            .filter(d -> d.getRideType().equals(request.getRideType()))
            .sorted(Comparator.comparing(d -> 
                d.getLocation().distanceTo(request.getPickupLocation())))
            .limit(10)
            .collect(Collectors.toList());
        
        if (availableDrivers.isEmpty()) {
            throw new NoDriversAvailableException();
        }
        
        // Send notifications to top 3 drivers
        return sendMatchRequests(request, availableDrivers.subList(0, 3));
    }
    
    private CompletableFuture<RideMatch> sendMatchRequests(
            RideRequest request, 
            List<Driver> drivers) {
        
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<RideMatch> future = new CompletableFuture<>();
        
        // Store request in Redis with TTL
        redisTemplate.opsForValue().set(
            "ride:request:" + requestId,
            objectMapper.writeValueAsString(request),
            Duration.ofMinutes(5)
        );
        
        // Send push notifications to drivers
        for (Driver driver : drivers) {
            notificationService.sendRideRequest(driver.getId(), request);
        }
        
        // Listen for driver acceptance
        listenForAcceptance(requestId, future);
        
        return future;
    }
    
    @KafkaListener(topics = "ride-acceptance")
    public void handleDriverAcceptance(RideAcceptance acceptance) {
        String requestId = acceptance.getRequestId();
        
        // Use Redis distributed lock to ensure only one driver accepts
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
            "lock:ride:" + requestId,
            acceptance.getDriverId(),
            Duration.ofSeconds(30)
        );
        
        if (Boolean.TRUE.equals(locked)) {
            // This driver got the ride
            createRide(acceptance);
            
            // Notify other drivers that ride is taken
            notifyRideTaken(requestId, acceptance.getDriverId());
        } else {
            // Ride already taken
            notificationService.sendRideTaken(acceptance.getDriverId());
        }
    }
}
```

---

### 4.3 Fare Calculation Service

```java
@Service
public class FareCalculationService {
    
    public FareEstimate calculateFare(RideRequest request) {
        // Get route details
        RouteInfo route = mapsService.getRoute(
            request.getPickupLocation(),
            request.getDropoffLocation()
        );
        
        // Base fare calculation
        double baseFare = calculateBaseFare(request.getRideType());
        double distanceFare = route.getDistanceKm() * getPerKmRate(request.getRideType());
        double timeFare = route.getDurationMinutes() * getPerMinuteRate(request.getRideType());
        
        double subtotal = baseFare + distanceFare + timeFare;
        
        // Apply surge pricing
        double surgeMultiplier = getSurgeMultiplier(
            request.getPickupLocation(),
            LocalDateTime.now()
        );
        
        double total = subtotal * surgeMultiplier;
        
        // Apply promo code if any
        if (request.getPromoCode() != null) {
            total = applyPromoCode(total, request.getPromoCode());
        }
        
        return FareEstimate.builder()
            .baseFare(baseFare)
            .distanceFare(distanceFare)
            .timeFare(timeFare)
            .surgeMultiplier(surgeMultiplier)
            .total(total)
            .estimatedDuration(route.getDurationMinutes())
            .build();
    }
    
    // Surge pricing based on supply-demand
    private double getSurgeMultiplier(Location location, LocalDateTime time) {
        String geohash = geohashService.encode(location, 6);
        
        // Get demand (ride requests) and supply (available drivers)
        long demand = redisTemplate.opsForValue()
            .increment("demand:" + geohash + ":" + time.getHour());
        
        long supply = redisTemplate.opsForGeo()
            .radius("drivers:active", 
                   new Circle(location, new Distance(2, Metrics.KILOMETERS)))
            .getContent().size();
        
        double ratio = (double) demand / Math.max(supply, 1);
        
        // Surge multiplier: 1.0x to 3.0x
        if (ratio < 1.0) return 1.0;
        if (ratio < 2.0) return 1.5;
        if (ratio < 3.0) return 2.0;
        return 3.0;
    }
}
```

---

### 4.4 Payment Service

```java
@Service
public class PaymentService {
    
    private final StripeClient stripeClient;
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    
    @Transactional
    public Payment processPayment(Ride ride) {
        Payment payment = Payment.builder()
            .rideId(ride.getId())
            .amount(ride.getFare())
            .status(PaymentStatus.PENDING)
            .idempotencyKey(UUID.randomUUID().toString())
            .build();
        
        paymentRepository.save(payment);
        
        try {
            // Process payment with Stripe
            PaymentIntent intent = stripeClient.createPaymentIntent(
                PaymentIntentCreateParams.builder()
                    .setAmount((long) (ride.getFare() * 100)) // cents
                    .setCurrency("usd")
                    .setCustomer(ride.getRider().getStripeCustomerId())
                    .setPaymentMethod(ride.getPaymentMethodId())
                    .setConfirm(true)
                    .setIdempotencyKey(payment.getIdempotencyKey())
                    .build()
            );
            
            if ("succeeded".equals(intent.getStatus())) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(intent.getId());
                paymentRepository.save(payment);
                
                // Publish payment success event
                kafkaTemplate.send("payment-events", 
                    new PaymentSuccessEvent(ride.getId(), payment.getId()));
                
                // Transfer to driver (async)
                transferToDriver(ride.getDriver(), ride.getFare() * 0.8); // 80% to driver
                
                return payment;
            } else {
                throw new PaymentFailedException("Payment failed: " + intent.getStatus());
            }
            
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(e.getMessage());
            paymentRepository.save(payment);
            
            kafkaTemplate.send("payment-events",
                new PaymentFailedEvent(ride.getId(), e.getMessage()));
            
            throw new PaymentFailedException(e);
        }
    }
    
    // Idempotent payment processing
    public Payment processPaymentIdempotent(Ride ride, String idempotencyKey) {
        // Check if payment already processed
        Optional<Payment> existing = paymentRepository
            .findByIdempotencyKey(idempotencyKey);
        
        if (existing.isPresent()) {
            return existing.get(); // Return existing payment
        }
        
        return processPayment(ride);
    }
}
```

---

## 5. Database Schema

### PostgreSQL (Transactional Data)

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL, -- RIDER, DRIVER
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);

-- Drivers table
CREATE TABLE drivers (
    id UUID PRIMARY KEY REFERENCES users(id),
    license_number VARCHAR(50) UNIQUE NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    vehicle_number VARCHAR(20) NOT NULL,
    rating DECIMAL(3,2) DEFAULT 5.0,
    total_rides INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'OFFLINE', -- ONLINE, OFFLINE, ON_RIDE
    current_location GEOGRAPHY(POINT),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_drivers_status ON drivers(status);
CREATE INDEX idx_drivers_location ON drivers USING GIST(current_location);

-- Rides table
CREATE TABLE rides (
    id UUID PRIMARY KEY,
    rider_id UUID REFERENCES users(id),
    driver_id UUID REFERENCES drivers(id),
    pickup_location GEOGRAPHY(POINT) NOT NULL,
    dropoff_location GEOGRAPHY(POINT) NOT NULL,
    pickup_address TEXT,
    dropoff_address TEXT,
    status VARCHAR(20) NOT NULL, -- REQUESTED, ACCEPTED, STARTED, COMPLETED, CANCELLED
    ride_type VARCHAR(20) NOT NULL, -- UBERX, UBERXL, UBERPOOL
    fare DECIMAL(10,2),
    distance_km DECIMAL(10,2),
    duration_minutes INTEGER,
    surge_multiplier DECIMAL(3,2) DEFAULT 1.0,
    requested_at TIMESTAMP DEFAULT NOW(),
    accepted_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP
);

CREATE INDEX idx_rides_rider ON rides(rider_id);
CREATE INDEX idx_rides_driver ON rides(driver_id);
CREATE INDEX idx_rides_status ON rides(status);
CREATE INDEX idx_rides_requested_at ON rides(requested_at);

-- Payments table
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    ride_id UUID REFERENCES rides(id),
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, COMPLETED, FAILED, REFUNDED
    payment_method VARCHAR(50),
    transaction_id VARCHAR(255),
    idempotency_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_payments_ride ON payments(ride_id);
CREATE INDEX idx_payments_idempotency ON payments(idempotency_key);
```

### Cassandra (Location History)

```sql
-- Location updates (time-series data)
CREATE TABLE location_updates (
    driver_id UUID,
    timestamp TIMESTAMP,
    latitude DOUBLE,
    longitude DOUBLE,
    speed DOUBLE,
    heading DOUBLE,
    PRIMARY KEY (driver_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);

-- Ride tracking
CREATE TABLE ride_tracking (
    ride_id UUID,
    timestamp TIMESTAMP,
    latitude DOUBLE,
    longitude DOUBLE,
    PRIMARY KEY (ride_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);
```

---

## 6. API Design

```java
// Rider APIs
POST   /api/v1/rides/request
GET    /api/v1/rides/{rideId}
DELETE /api/v1/rides/{rideId}/cancel
GET    /api/v1/rides/history
POST   /api/v1/rides/{rideId}/rating

// Driver APIs
GET    /api/v1/drivers/nearby-requests
POST   /api/v1/drivers/rides/{rideId}/accept
POST   /api/v1/drivers/rides/{rideId}/start
POST   /api/v1/drivers/rides/{rideId}/complete
PUT    /api/v1/drivers/location
GET    /api/v1/drivers/earnings

// WebSocket for real-time updates
WS     /ws/location
WS     /ws/ride-updates
```

---

## 7. Trade-offs & Optimizations

### Trade-offs

1. **Consistency vs Availability**
   - Location updates: Eventual consistency (AP)
   - Payment processing: Strong consistency (CP)

2. **Latency vs Accuracy**
   - QuadTree: Fast but approximate
   - Exact distance calculation: Slow but accurate

3. **Cost vs Performance**
   - Redis for hot data (expensive but fast)
   - Cassandra for historical data (cheaper, slower)

### Optimizations

1. **Caching Strategy**
   - Driver locations: Redis (TTL 30 seconds)
   - Fare estimates: Redis (TTL 5 minutes)
   - User profiles: Redis (TTL 1 hour)

2. **Database Sharding**
   - Shard rides by rider_id
   - Shard location data by geohash

3. **Connection Pooling**
   - WebSocket connections for real-time updates
   - HTTP/2 for API calls

---

## 8. Monitoring & Observability

```java
@Component
public class RideMetrics {
    
    @Timed(value = "ride.matching.time")
    public RideMatch matchRide(RideRequest request) {
        // Matching logic
    }
    
    @Counted(value = "ride.requests.total")
    public void recordRideRequest() {
        // Increment counter
    }
    
    @Gauge(value = "drivers.active.count")
    public long getActiveDriversCount() {
        return driverService.getActiveCount();
    }
}
```

**Key Metrics:**
- Ride matching time (p50, p95, p99)
- Active drivers count
- Ride completion rate
- Payment success rate
- Average surge multiplier

---

**Interview Tips:**
1. Start with geospatial indexing (QuadTree/Geohash)
2. Explain real-time matching algorithm
3. Discuss surge pricing logic
4. Mention payment idempotency
5. Talk about WebSocket for real-time updates

---

**Next:** [Design Netflix System](../02-netflix-system/README.md)
