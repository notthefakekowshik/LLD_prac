# Parking Lot System

A thread-safe, multi-level parking lot with pluggable spot-allocation and fee-calculation strategies.

## Quick Start

```java
// Build the lot (2 levels, 16 spots)
ParkingLotService service = new ParkingLotServiceImpl(
    parkingLot,
    new NearestSpotStrategy(),
    new HourlyFeeStrategy(),
    new InMemoryTicketRepository()
);

// Check in
Vehicle car = VehicleFactory.create(VehicleType.CAR, "MH-01-AB-1234");
Ticket ticket = service.checkIn(car);

// Query availability
long freeCarSpots = service.getAvailableSpots(VehicleType.CAR);

// Check out
double fee = service.checkOut(ticket.getTicketId());
```

## Supported Vehicle / Spot Types

| Vehicle | Preferred spot | Can overflow to |
|---------|---------------|-----------------|
| BIKE    | COMPACT       | REGULAR, LARGE  |
| CAR     | REGULAR       | LARGE           |
| TRUCK   | LARGE         | —               |

`NearestSpotStrategy` allocates the **smallest compatible spot** first (avoids wasting large spots on small vehicles) and scans levels in order (nearest to entry first). If the preferred type is full across all levels, it overflows to the next-compatible size.

## Fee Calculation (HourlyFeeStrategy)

| Vehicle | Rate/hour | Minimum |
|---------|-----------|---------|
| BIKE    | ₹20       | ₹10     |
| CAR     | ₹50       | ₹10     |
| TRUCK   | ₹100      | ₹10     |

Partial hours are rounded up (ceiling billing).

## Thread Safety

| Operation | Mechanism |
|-----------|-----------|
| `checkIn` | `ReentrantLock` guards allocate + park as one atomic unit |
| `checkOut` | `ConcurrentHashMap.remove()` is atomic — only one caller wins per ticket |
| `park / vacate` | `synchronized` on `ParkingSpot` |
| Metrics | `AtomicLong` fields — always consistent |

**Double-booking prevention:** `checkIn` is fully serialised via `checkInLock`. Two threads cannot both see the same spot as AVAILABLE and both park in it.

**Double-checkout prevention:** `ticketRepository.removeAndGet()` uses `ConcurrentHashMap.remove()` which is atomic. The second thread gets an empty Optional and throws `InvalidTicketException`.

## Extending the System

### Add EV charging spots

```java
// 1. Add EV to SpotType enum
public enum SpotType { COMPACT, REGULAR, LARGE, EV; ... }

// 2. Write a new strategy — zero changes to existing classes
public class EVPreferenceStrategy implements SpotAllocationStrategy {
    @Override
    public Optional<ParkingSpot> allocate(Vehicle vehicle, List<ParkingLevel> levels) {
        if (vehicle instanceof ElectricVehicle) {
            // prefer EV spots first
        }
        // fall back to NearestSpotStrategy logic
    }
}

// 3. Inject at build time
new ParkingLotServiceImpl(lot, new EVPreferenceStrategy(), feeStrategy, repo);
```

### Swap to flat-rate billing

```java
public class FlatRateFeeStrategy implements FeeCalculationStrategy {
    @Override
    public double calculateFee(Ticket ticket, LocalDateTime exitTime) {
        return 150.0; // flat ₹150 regardless of duration
    }
}
```

## Design Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **Facade** | `ParkingLotService` | Single API hides levels, spots, allocation, fees |
| **Strategy (×2)** | `SpotAllocationStrategy`, `FeeCalculationStrategy` | Swap algorithms without touching the service |
| **Factory Method** | `VehicleFactory` | OCP — new vehicle type = new subclass, no edits to callers |

## Demo

`ParkingLotDemo.main()` covers all 9 functional requirements including a concurrent check-in test with 10 threads racing to enter simultaneously.
