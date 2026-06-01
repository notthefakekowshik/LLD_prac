# Movie Booking System (BookMyShow)

A concurrent seat-booking system demonstrating **Facade**, **Strategy**, and **Observer** patterns with pessimistic seat-level locking.

## Features

- **Search shows** by city, movie name, and date
- **View seat availability** per show with real-time lock status
- **Lock seats** atomically — all-or-nothing seat reservation
- **Confirm booking** with mocked payment
- **Lock expiry** — seats auto-release after configurable timeout
- **Cancel booking** — seats return to available pool
- **Concurrent booking prevention** — no two users can book the same seat
- **Thread-safe** — pessimistic show-level lock ensures atomic check-and-reserve

## Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Facade** | `BookingFacade` | Single entry point hides ShowRepository, BookingRepository, SeatLockService, PaymentService |
| **Strategy** | `SeatLockService` — expiry via sweep or poll-on-access | Swap lock expiry strategy without touching booking logic |
| **Observer** | `BookingEventListener` (interface, ready for integration) | Decouple post-booking actions |
| **Repository** | `ShowRepository`, `BookingRepository` | In-memory data stores; swappable for DB |

## Quick Start

```bash
mvn compile exec:java -Dexec.mainClass="com.lldprep.systems.moviebooking.demo.MovieBookingDemo" -pl core-lld
```

## Package Structure

```
com.lldprep.systems.moviebooking/
├── model/
│   ├── Seat.java                  # Row + col → seatId (A1, B3...)
│   ├── Show.java                  # Movie + screen + time + price
│   ├── Screen.java               # Layout: rows × cols → generates seats
│   ├── Theater.java              # Name + city + list of screens
│   ├── Booking.java              # User + show + seats + status
│   ├── SeatLockInfo.java         # userId + lockedAt + expiresAt
│   └── enums/
│       ├── City.java
│       ├── SeatStatus.java
│       └── BookingStatus.java
├── repository/
│   ├── ShowRepository.java       # Search + register theaters/shows
│   └── BookingRepository.java    # CRUD operations
├── service/
│   ├── BookingFacade.java        # FACADE: single entry point
│   ├── SeatLockService.java      # PESSIMISTIC LOCKING: per-show sync
│   └── PaymentService.java       # Mocked payment processing
├── exception/
│   ├── BookingException.java
│   ├── SeatLockException.java
│   └── PaymentException.java
└── demo/
    └── MovieBookingDemo.java     # 8 scenarios
```

## Concurrency Model

**Pessimistic show-level lock** — all check-and-lock operations for a show are serialized:

```
lockSeats(showId, userId, seatIds):
  synchronized (showLockObjects[showId]) {
    for each seatId:
      if locked by someone else and not expired → return false
    for each seatId:
      locks[showId][seatId] = LockInfo(userId, now, now+5min)
    return true
  }
```

**Why pessimistic over optimistic?** Optimistic retry loops waste time under contention. Pessimistic is correct by construction. Lock duration is microseconds (HashMap ops), so contention is negligible.

## Demo Scenarios

1. **Search** — Inception in Bangalore today → 5 shows across 2 theaters
2. **View seats** — 80 seats available on Screen 1
3. **Happy path** — Lock A1-A3 → Confirm → Pay ₹750
4. **Concurrent locking** — Bob and Charlie try same seats → only one succeeds
5. **Lock expiry** — 2-second lock expires → seats return → another user locks them
6. **Cancel booking** — Confirm then cancel → seats are freed
7. **Double-booking prevention** — User locks seats → another user fails to lock same
8. **No-conflict concurrent** — Two users book different seats → both succeed

## Example Usage

```java
BookingFacade facade = new BookingFacade(showRepo, bookingRepo, lockService, paymentService);

// Search
List<Show> shows = facade.searchShows(City.BANGALORE, "Inception", LocalDate.now());

// Lock seats
boolean locked = facade.lockSeats(showId, "user-alice", List.of("A1", "A2", "A3"));

// Confirm
Booking booking = facade.confirmBooking(showId, "user-alice");

// Cancel
facade.cancelBooking(booking.getBookingId());
```

## Extending the System

| Curveball | Extension |
|-----------|----------|
| Seat categories (Gold/Silver) | `Seat` gains `category`; `Show` holds price map |
| Cancellation refund | `RefundPolicy` Strategy interface |
| Real payment gateway | `PaymentService` → Adapter pattern |
| Show recommendations | New `RecommendationService` |
| Seat preferences (aisle) | `Seat` gains `List<String> tags` |

## Documentation

- `DESIGN.md` — Full D.I.C.E. workflow with class diagram and design decisions
- `SCHEMA.md` — Database ER diagram, 8 tables, concurrency model
- `API_CONTRACT.md` — REST endpoints, request/response, error codes

---

**Completed:** 2026-05-31 | **Patterns:** Facade, Strategy, Observer, Repository
