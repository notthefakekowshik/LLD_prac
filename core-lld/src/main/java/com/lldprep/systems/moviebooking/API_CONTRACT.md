# Movie Booking System (BookMyShow) — API Contract

REST API for the movie booking system. User-facing, stateful booking flow.

---

## Endpoint Summary

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/shows` | Search shows by city, movie, date |
| `GET` | `/api/v1/shows/{showId}/seats` | View available seats for a show |
| `POST` | `/api/v1/shows/{showId}/locks` | Lock selected seats |
| `POST` | `/api/v1/shows/{showId}/bookings` | Confirm booking (pay) |
| `DELETE` | `/api/v1/bookings/{bookingId}` | Cancel a booking |
| `GET` | `/api/v1/bookings/{bookingId}` | Get booking details |
| `GET` | `/api/v1/users/{userId}/bookings` | Get user's booking history |

---

## 1. Search Shows

```
GET /api/v1/shows?city=BANGALORE&movie=Inception&date=2026-06-01
```

**Response (200):**
```json
{
  "results": [
    {
      "showId": "SHOW-A1B2C3",
      "movieName": "Inception",
      "theaterName": "PVR Koramangala",
      "screenName": "Screen 1",
      "showTime": "2026-06-01T10:00:00",
      "durationMinutes": 148,
      "basePrice": 250.00,
      "availableSeats": 77
    },
    {
      "showId": "SHOW-D4E5F6",
      "movieName": "Inception",
      "theaterName": "INOX MG Road",
      "screenName": "Audi 1",
      "showTime": "2026-06-01T10:30:00",
      "durationMinutes": 148,
      "basePrice": 220.00,
      "availableSeats": 120
    }
  ]
}
```

| Query Param | Type | Required | Notes |
|-------------|------|----------|-------|
| `city` | `String` | Yes | Enum: BANGALORE, MUMBAI, DELHI, HYDERABAD, CHENNAI |
| `movie` | `String` | No | Case-insensitive partial match |
| `date` | `String` | Yes | Format: `YYYY-MM-DD` |

---

## 2. View Available Seats

```
GET /api/v1/shows/{showId}/seats
```

**Response (200):**
```json
{
  "showId": "SHOW-A1B2C3",
  "screenName": "Screen 1",
  "totalSeats": 80,
  "availableSeats": 77,
  "bookedSeats": ["A1", "A2", "A3"],
  "lockedSeats": [],
  "layout": {
    "rows": 8,
    "cols": 10,
    "seats": [
      {"seatId": "A1", "row": 0, "col": 0, "status": "BOOKED"},
      {"seatId": "A2", "row": 0, "col": 1, "status": "BOOKED"},
      {"seatId": "A3", "row": 0, "col": 2, "status": "BOOKED"},
      {"seatId": "A4", "row": 0, "col": 3, "status": "AVAILABLE"}
    ]
  }
}
```

---

## 3. Lock Seats

```
POST /api/v1/shows/{showId}/locks
Content-Type: application/json
```

**Request:**
```json
{
  "userId": "user-alice",
  "seatIds": ["A4", "A5", "A6"]
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `userId` | `String` | Yes | Non-empty |
| `seatIds` | `String[]` | Yes | 1-10 seats, valid seat IDs for this screen |

**Success Response (200):**
```json
{
  "success": true,
  "lockedSeats": ["A4", "A5", "A6"],
  "expiresAt": "2026-06-01T14:05:00",
  "totalAmount": 750.00
}
```

**Error Responses:**

| Status | Error Code | Message |
|--------|-----------|---------|
| `409` | `SEATS_UNAVAILABLE` | Seats [A4] are already locked |
| `404` | `SHOW_NOT_FOUND` | Show SHOW-XYZ not found |
| `400` | `INVALID_SEATS` | Seat "Z99" does not exist in this screen |

---

## 4. Confirm Booking (Pay)

```
POST /api/v1/shows/{showId}/bookings
Content-Type: application/json
```

**Request:**
```json
{
  "userId": "user-alice",
  "paymentMethod": "CREDIT_CARD"
}
```

**Success Response (201):**
```json
{
  "bookingId": "BK-672AF8F9",
  "userId": "user-alice",
  "showId": "SHOW-A1B2C3",
  "seats": ["A4", "A5", "A6"],
  "totalAmount": 750.00,
  "status": "CONFIRMED",
  "createdAt": "2026-06-01T14:00:00"
}
```

**Error Responses:**

| Status | Error Code | Message |
|--------|-----------|---------|
| `400` | `NO_SEATS_LOCKED` | You have no locked seats for this show |
| `402` | `PAYMENT_FAILED` | Payment failed — seats have been released |
| `408` | `LOCK_EXPIRED` | Your seat lock has expired — seats released |

---

## 5. Cancel Booking

```
DELETE /api/v1/bookings/{bookingId}
```

**Request:**
```json
{
  "userId": "user-alice"
}
```

**Success Response (200):**
```json
{
  "bookingId": "BK-672AF8F9",
  "status": "CANCELLED",
  "message": "Booking cancelled. Seats released."
}
```

**Error Responses:**

| Status | Error Code | Message |
|--------|-----------|---------|
| `404` | `BOOKING_NOT_FOUND` | Booking not found |
| `400` | `ALREADY_CANCELLED` | Booking is already cancelled |
| `403` | `NOT_OWNER` | Booking belongs to different user |

---

## 6. Booking Details

```
GET /api/v1/bookings/{bookingId}
```

**Response (200):**
```json
{
  "bookingId": "BK-672AF8F9",
  "userId": "user-alice",
  "movieName": "Inception",
  "theaterName": "PVR Koramangala",
  "screenName": "Screen 1",
  "showTime": "2026-06-01T10:00:00",
  "seats": ["A4", "A5", "A6"],
  "totalAmount": 750.00,
  "status": "CONFIRMED",
  "createdAt": "2026-06-01T14:00:00"
}
```

---

## Booking Flow (Postman-Style)

```
Happy Path:
  1. GET  /api/v1/shows?city=BANGALORE&movie=Inception&date=2026-06-01  → pick show
  2. GET  /api/v1/shows/SHOW-A1B2C3/seats                                 → see seats
  3. POST /api/v1/shows/SHOW-A1B2C3/locks   {"userId":"A","seatIds":["A4","A5"]}  → lock
  4. POST /api/v1/shows/SHOW-A1B2C3/bookings {"userId":"A","payment":"CARD"}      → book

Concurrent Conflict:
  3a. POST /api/v1/shows/SHOW-A1B2C3/locks   {"userId":"A","seatIds":["A4","A5"]}  → 200, locked
  3b. POST /api/v1/shows/SHOW-A1B2C3/locks   {"userId":"B","seatIds":["A4","A6"]}  → 409, A4 already locked

Lock Expiry:
  3a. POST .../locks → 200 with lock expiresAt in 5 min
  (wait 5 minutes)
  3b. POST .../bookings with same userId → 408 LOCK_EXPIRED
  3c. POST .../locks with different userId → 200 (seats returned)
```

---

## DTO → Model Mapping

| API Field | In-Memory Entity |
|-----------|-----------------|
| `userId` | `Booking.userId`, `SeatLockInfo.userId` |
| `seatIds` | `Seat.seatId` (generated from row+col) |
| `showId` | `Show.id` |
| `bookingId` | `Booking.bookingId` |
| `city` query param | `City` enum |
| `movieName` | `Show.movieName` |
| `status` | `BookingStatus` enum |
