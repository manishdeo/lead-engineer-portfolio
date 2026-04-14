# 🚗 Design Uber/Ride-Sharing System

## System Requirements

### Functional Requirements
- Users can request rides
- Drivers can accept/reject ride requests
- Real-time location tracking
- Fare calculation and payment
- Trip history and ratings

### Non-Functional Requirements
- 100M+ users, 1M+ drivers
- 1M+ rides per day
- 99.9% availability
- Real-time updates (< 1 second)
- Global scale

## High-Level Architecture

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Mobile    │    │     Web     │    │   Driver    │
│     App     │    │   Portal    │    │     App     │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                  ┌─────────────┐
                  │ Load Balancer│
                  │   (ALB)     │
                  └─────────────┘
                           │
                  ┌─────────────┐
                  │ API Gateway │
                  │  (Kong)     │
                  └─────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│    User     │    │    Trip     │    │   Driver    │
│   Service   │    │   Service   │    │   Service   │
└─────────────┘    └─────────────┘    └─────────────┘
        │                  │                  │
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   User DB   │    │   Trip DB   │    │  Driver DB  │
│(PostgreSQL) │    │(PostgreSQL) │    │(PostgreSQL) │
└─────────────┘    └─────────────┘    └─────────────┘
```

## Core Services Implementation

### 1. Location Service (Real-time tracking)

```java
@RestController
@RequestMapping("/api/location")
public class LocationController {
    
    @Autowired
    private LocationService locationService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Update driver location
    @PostMapping("/driver/{driverId}")
    public ResponseEntity<Void> updateDriverLocation(
            @PathVariable String driverId,
            @RequestBody LocationUpdate locationUpdate) {
        
        // Store in Redis for fast access
        String key = "driver:location:" + driverId;
        DriverLocation location = DriverLocation.builder()
            .driverId(driverId)
            .latitude(locationUpdate.getLatitude())
            .longitude(locationUpdate.getLongitude())
            .timestamp(Instant.now())
            .build();
            
        redisTemplate.opsForValue().set(key, location, Duration.ofMinutes(5));
        
        // Update geospatial index
        redisTemplate.opsForGeo().add("drivers:geo", 
            new Point(location.getLongitude(), location.getLatitude()), 
            driverId);
        
        // Publish to WebSocket for real-time updates
        locationService.broadcastLocationUpdate(location);
        
        return ResponseEntity.ok().build();
    }
    
    // Find nearby drivers
    @GetMapping("/drivers/nearby")
    public ResponseEntity<List<NearbyDriver>> findNearbyDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") double radiusKm) {
        
        Point center = new Point(longitude, latitude);
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
        
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = 
            redisTemplate.opsForGeo().radius("drivers:geo", center, radius);
        
        List<NearbyDriver> nearbyDrivers = results.getContent().stream()
            .map(result -> {
                String driverId = result.getContent().getName();
                double distance = result.getDistance().getValue();
                
                // Get driver details from cache
                DriverLocation location = (DriverLocation) redisTemplate
                    .opsForValue().get("driver:location:" + driverId);
                
                return NearbyDriver.builder()
                    .driverId(driverId)
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .distanceKm(distance)
                    .build();
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(nearbyDrivers);
    }
}

@Service
public class LocationService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public void broadcastLocationUpdate(DriverLocation location) {
        // Send to specific trip subscribers
        messagingTemplate.convertAndSend(
            "/topic/location/" + location.getDriverId(), 
            location
        );
    }
}
```

### 2. Trip Matching Service

```java
@Service
public class TripMatchingService {
    
    @Autowired
    private LocationService locationService;
    
    @Autowired
    private DriverService driverService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public TripMatchResult requestRide(RideRequest request) {
        // 1. Find nearby available drivers
        List<NearbyDriver> nearbyDrivers = locationService.findNearbyDrivers(
            request.getPickupLatitude(),
            request.getPickupLongitude(),
            5.0 // 5km radius
        );
        
        // 2. Filter available drivers
        List<Driver> availableDrivers = nearbyDrivers.stream()
            .map(nd -> driverService.getDriver(nd.getDriverId()))
            .filter(driver -> driver.getStatus() == DriverStatus.AVAILABLE)
            .collect(Collectors.toList());
        
        if (availableDrivers.isEmpty()) {
            return TripMatchResult.noDriversAvailable();
        }
        
        // 3. Apply matching algorithm (closest first for simplicity)
        Driver selectedDriver = availableDrivers.get(0);
        
        // 4. Create trip
        Trip trip = Trip.builder()
            .id(UUID.randomUUID().toString())
            .userId(request.getUserId())
            .driverId(selectedDriver.getId())
            .pickupLatitude(request.getPickupLatitude())
            .pickupLongitude(request.getPickupLongitude())
            .destinationLatitude(request.getDestinationLatitude())
            .destinationLongitude(request.getDestinationLongitude())
            .status(TripStatus.REQUESTED)
            .requestedAt(Instant.now())
            .build();
        
        // 5. Send trip request to driver
        TripRequestEvent event = TripRequestEvent.builder()
            .tripId(trip.getId())
            .driverId(selectedDriver.getId())
            .userId(request.getUserId())
            .pickupLocation(new Location(request.getPickupLatitude(), request.getPickupLongitude()))
            .destinationLocation(new Location(request.getDestinationLatitude(), request.getDestinationLongitude()))
            .build();
        
        kafkaTemplate.send("trip-requests", event);
        
        return TripMatchResult.success(trip);
    }
}
```

### 3. Real-time Communication (WebSocket)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}

@Controller
public class TripWebSocketController {
    
    @Autowired
    private TripService tripService;
    
    // Driver accepts/rejects trip
    @MessageMapping("/trip/{tripId}/response")
    public void handleTripResponse(
            @DestinationVariable String tripId,
            TripResponse response,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String driverId = headerAccessor.getUser().getName();
        
        if (response.isAccepted()) {
            tripService.acceptTrip(tripId, driverId);
            
            // Notify user
            messagingTemplate.convertAndSend(
                "/queue/trip-updates/" + response.getUserId(),
                TripUpdate.builder()
                    .tripId(tripId)
                    .status(TripStatus.ACCEPTED)
                    .driverId(driverId)
                    .build()
            );
        } else {
            tripService.rejectTrip(tripId, driverId);
            // Find alternative driver
        }
    }
    
    // Real-time location updates during trip
    @MessageMapping("/trip/{tripId}/location")
    public void updateTripLocation(
            @DestinationVariable String tripId,
            LocationUpdate locationUpdate) {
        
        // Broadcast to user
        messagingTemplate.convertAndSend(
            "/topic/trip/" + tripId + "/location",
            locationUpdate
        );
    }
}
```

### 4. Pricing Service

```java
@Service
public class PricingService {
    
    private static final double BASE_FARE = 2.50;
    private static final double PER_KM_RATE = 1.20;
    private static final double PER_MINUTE_RATE = 0.25;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public FareEstimate calculateFare(FareRequest request) {
        // 1. Calculate distance using external service (Google Maps API)
        double distanceKm = calculateDistance(
            request.getPickupLatitude(), request.getPickupLongitude(),
            request.getDestinationLatitude(), request.getDestinationLongitude()
        );
        
        // 2. Estimate duration
        double estimatedMinutes = estimateTime(
            request.getPickupLatitude(), request.getPickupLongitude(),
            request.getDestinationLatitude(), request.getDestinationLongitude()
        );
        
        // 3. Apply surge pricing
        double surgeMultiplier = getSurgeMultiplier(
            request.getPickupLatitude(), request.getPickupLongitude()
        );
        
        // 4. Calculate fare
        double baseFare = BASE_FARE;
        double distanceFare = distanceKm * PER_KM_RATE;
        double timeFare = estimatedMinutes * PER_MINUTE_RATE;
        
        double totalFare = (baseFare + distanceFare + timeFare) * surgeMultiplier;
        
        return FareEstimate.builder()
            .baseFare(baseFare)
            .distanceFare(distanceFare)
            .timeFare(timeFare)
            .surgeMultiplier(surgeMultiplier)
            .totalFare(totalFare)
            .estimatedDuration(estimatedMinutes)
            .build();
    }
    
    private double getSurgeMultiplier(double latitude, double longitude) {
        // Get surge pricing from Redis based on location
        String geoHash = GeoHash.withCharacterPrecision(latitude, longitude, 6).toBase32();
        String key = "surge:" + geoHash;
        
        Double surge = (Double) redisTemplate.opsForValue().get(key);
        return surge != null ? surge : 1.0;
    }
}
```

### 5. Payment Service Integration

```java
@Service
public class PaymentService {
    
    @Autowired
    private StripePaymentGateway stripeGateway;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Transactional
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            // 1. Create payment record
            Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .tripId(request.getTripId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();
            
            payment = paymentRepository.save(payment);
            
            // 2. Process with payment gateway
            StripePaymentResult stripeResult = stripeGateway.charge(
                request.getPaymentMethodId(),
                request.getAmount(),
                "Trip payment for " + request.getTripId()
            );
            
            // 3. Update payment status
            if (stripeResult.isSuccessful()) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setGatewayTransactionId(stripeResult.getTransactionId());
                payment.setCompletedAt(Instant.now());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(stripeResult.getErrorMessage());
            }
            
            paymentRepository.save(payment);
            
            return PaymentResult.builder()
                .paymentId(payment.getId())
                .successful(stripeResult.isSuccessful())
                .transactionId(stripeResult.getTransactionId())
                .build();
                
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            return PaymentResult.failed("Payment processing error");
        }
    }
}
```

## Database Schema

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Drivers table
CREATE TABLE drivers (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    license_number VARCHAR(50) UNIQUE NOT NULL,
    vehicle_id UUID REFERENCES vehicles(id),
    status VARCHAR(20) DEFAULT 'OFFLINE',
    rating DECIMAL(3,2) DEFAULT 5.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trips table
CREATE TABLE trips (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    driver_id UUID REFERENCES drivers(id),
    pickup_latitude DECIMAL(10,8) NOT NULL,
    pickup_longitude DECIMAL(11,8) NOT NULL,
    destination_latitude DECIMAL(10,8) NOT NULL,
    destination_longitude DECIMAL(11,8) NOT NULL,
    status VARCHAR(20) NOT NULL,
    fare_amount DECIMAL(10,2),
    distance_km DECIMAL(8,2),
    duration_minutes INTEGER,
    requested_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_trips_user_id ON trips(user_id);
CREATE INDEX idx_trips_driver_id ON trips(driver_id);
CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_trips_requested_at ON trips(requested_at);
```

## Scalability Considerations

### 1. Database Sharding
- Shard by geographic regions
- User service: shard by user_id
- Trip service: shard by pickup location

### 2. Caching Strategy
- Redis for real-time location data
- Driver availability status
- Surge pricing data
- User session data

### 3. Message Queue (Kafka)
- Trip request events
- Location updates
- Payment events
- Notification events

### 4. Load Balancing
- Geographic load balancing
- Service-specific load balancers
- WebSocket connection balancing

## Monitoring & Observability

```yaml
# Prometheus metrics
trip_requests_total{status="success|failed"}
driver_response_time_seconds
location_update_frequency
payment_processing_duration_seconds
websocket_connections_active
```

This implementation covers the core components of a ride-sharing system with practical, interview-ready code examples.