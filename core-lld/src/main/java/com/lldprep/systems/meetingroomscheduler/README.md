# Meeting Room Scheduler

Meeting room booking system demonstrating **Strategy (Filter pipeline)**, **Repository**, and **Facade** patterns with BitSet-based O(1) slot availability checks.

## Features

- **Register users** and add meeting rooms with capacity, floor, and amenities
- **Search/filter rooms** by date + time slot, minimum capacity, floor, and required amenities — filter pipeline ordered by increasing cost
- **Book a room** for any 30-minute slot within working hours (9 AM – 6 PM)
- **Conflict detection** — tries to double-book the same slot returns a clear error
- **Cancel bookings** and free the slot for others
- **Room schedule** — full 18-slot day view with booked/free indicators
- **Thread-safe** — per-room `synchronized` on check-and-set prevents TOCTOU races

## Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Strategy** | `RoomFilter` → `CapacityFilter`, `FloorFilter`, `AmenityFilter`, `AvailabilityFilter` | Each search criterion is a separate class. New filter = new class, zero changes to search logic. |
| **Repository** | `UserRepository`, `RoomRepository`, `BookingRepository` | In-memory stores; swappable for DB |
| **Facade** | `MeetingSchedulerFacade` | Single entry point hiding filter pipeline, booking validation, and slot management |

## Quick Start

```bash
mvn compile exec:java -Dexec.mainClass="com.lldprep.systems.meetingroomscheduler.demo.MeetingSchedulerDemo" -pl core-lld
```

## Package Structure

```
com.lldprep.systems.meetingroomscheduler/
├── model/
│   ├── User.java                  # id, name, email
│   ├── Room.java                  # name, capacity, floor, amenities, schedule (BitSet per date)
│   ├── Booking.java               # room + user + date + slot range
│   └── Amenity.java               # PROJECTOR, WHITEBOARD, VIDEO_CONFERENCING, CONFERENCE_PHONE
├── policy/
│   ├── RoomFilter.java            # interface
│   ├── SearchCriteria.java        # record — date, slots, capacity, floor, amenities
│   ├── CapacityFilter.java
│   ├── FloorFilter.java
│   ├── AmenityFilter.java
│   └── AvailabilityFilter.java
├── service/
│   └── MeetingSchedulerFacade.java # FACADE: single entry point
├── repository/
│   ├── UserRepository.java
│   ├── RoomRepository.java
│   └── BookingRepository.java
├── exception/
│   ├── MeetingSchedulerException.java
│   ├── RoomNotFoundException.java
│   ├── UserNotFoundException.java
│   ├── BookingNotFoundException.java
│   ├── BookingConflictException.java
│   ├── InvalidBookingException.java
│   └── DuplicateEmailException.java
└── demo/
    └── MeetingSchedulerDemo.java
```

## Core Algorithm — BitSet Slot Availability

Each `Room` owns `Map<LocalDate, BitSet>`. Bit i = 1 means slot i is booked.

```
9:00 AM  = slot 0    10:00 AM = slot 2    11:00 AM = slot 4
9:30 AM  = slot 1    10:30 AM = slot 3    ...       5:30 PM = slot 17
```

**Check availability (O(1)):**
```java
BitSet dayBits = schedule.get(date);
if (dayBits == null) return true;                        // nothing booked
int firstSet = dayBits.nextSetBit(startSlot);
return firstSet == -1 || firstSet >= endSlot;            // no conflict in range
```

**Book (check-and-set, `synchronized`):**
```java
dayBits.set(startSlot, endSlot);   // set(start, end) is exclusive on end
```

**Free on cancel:**
```java
dayBits.clear(startSlot, endSlot);
```

## Concurrency Model

The `bookRoom` path is synchronised on the Room object — the `bookSlot` method is `synchronized`. This ensures the check-and-set is atomic, preventing TOCTOU (time-of-check-to-time-of-use) races.

```java
// Inside Room.java
public synchronized boolean bookSlot(LocalDate date, int startSlot, int endSlot) {
    // check ... set
}
```

Two threads booking different rooms proceed fully in parallel — they share no state. Only races on the same room are serialised.

## Demo Scenarios

1. **Register users** — Alice, Bob, Charlie
2. **Add rooms** — 4 rooms with different capacities, amenities, and floors
3. **Basic booking** — Alice books Conf A, 10:00–11:00 tomorrow
4. **Conflict detection** — Bob tries same room, 10:30–11:30 → rejected
5. **Non-overlapping booking** — Bob books 11:00–12:00 (adjacent, no overlap) → succeeds
6. **Room schedule view** — full 18-slot day display for any room
7. **Search with filters** — capacity ≥ 8, floor 1, projector required, free at 9:00–10:00
8. **Upcoming bookings** — Alice and Bob each see their future meetings
9. **Cancel and re-book** — Bob cancels, Charlie grabs the freed slot
10. **Validation failures** — past date, 45-min duration, outside working hours, duplicate email
11. **Concurrency** — 4 threads race for the same slot; exactly 1 wins

## Extending the System

| Curveball | Extension |
|-----------|----------|
| Attendee list | `Booking` gains `List<User> attendees`; capacity check includes attendee count |
| Room maintenance windows | `Room` gains `List<TimeSlot> blockedSlots`; `bookSlot` checks both |
| Multi-building | `Room` gains `buildingId`; new `BuildingFilter implements RoomFilter` |
| Recurring meetings | `RecurringBookingService` wraps facade, calls `bookRoom` on schedule |
| Admin approval flow | `Booking` gains `Status` enum; new `ApprovalService` |
| AI agent traffic | Rate limiter per user on `bookRoom`; agents retry on conflict |

## Documentation

- `DESIGN_DICE.md` — Full D.I.C.E. workflow, class diagram, BitSet algorithm, concurrency model
- `class_diagram.mermaid` — Standalone Mermaid.js class diagram
- `REQUIREMENTS.md` — Requirements-only spec (no implementation hints)
