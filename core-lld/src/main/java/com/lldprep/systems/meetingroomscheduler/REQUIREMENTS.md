# Meeting Room Scheduler — Requirements (DEFINE only)

> Design it yourself first. This file has zero implementation hints — no class names, no patterns, no data structures. Just the spec.

---

## Functional Requirements

1. A user can **register** with name and email.
2. An admin can **add a meeting room** with:
   - Name (e.g. "Conference Room A")
   - Capacity (max occupants)
   - Floor number
   - Amenities (projector, whiteboard, video conferencing, etc.)
3. Any user can **search/filter** available rooms by:
   - Date and time slot
   - Minimum capacity
   - Floor
   - Specific amenities (e.g. "must have projector AND whiteboard")
4. A user can **book a meeting room** for a specific date, start time, and duration.
   - **Invariant: no two meetings can overlap in the same room.** Attempting a conflicting booking must fail.
   - The user booking the room is the meeting organiser.
   - Start time and duration must fall within working hours (9:00 AM to 6:00 PM).
   - Meetings can span 30 minutes minimum, up to the full working day.
   - Duration must be in 30-minute increments (30, 60, 90, etc.).
   - Booking must be for a future date (today or later).
5. A user can **cancel** a booking they made.
6. A user can **view their upcoming bookings** — all future meetings they are organising or attending.
7. A user can **view a room's schedule** for a given date — all booked slots for that room.
8. (Optional) An admin can **remove a meeting room**.

---

## Non-Functional Requirements

- **Correctness:** No double bookings under any circumstance. This is the core invariant.
- **Thread-safe:** Two users trying to book the same room at overlapping times must not both succeed. Only one wins.
- **Efficient availability search:** Checking room availability for a time range should not require scanning all bookings linearly. Think about what lookup structure achieves better than O(n).
- **In-memory only** — no database, no persistence.
- **Single JVM process.**

---

## Constraints

- Working hours: 9:00 AM to 6:00 PM (9 hours).
- Time slots are in 30-minute increments (so there are 18 slots per day per room).
- Maximum advance booking: 30 days from today.
- Room capacity: 2 to 50 people.
- Meeting participants: organiser only (no attendee list required — keep it simple).
- No recurring meetings.

---

## Out of Scope

- User authentication / login.
- Meeting invitations / attendee management / RSVP.
- Recurring / repeating meetings.
- Resource booking (projector bulbs, catering, etc.).
- Overlap across floors or buildings — single office only.
- Notifications (email/push).
- Waitlist for conflicted slots.
- Room maintenance / blocked-off time.
- Multi-building / multi-office support.
- Calendar integration (Google/Outlook).
