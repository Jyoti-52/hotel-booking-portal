# DESIGN.md

## 1. Architecture

```
Angular SPA  ──HTTP/JSON──▶  Spring Boot REST API  ──JPA/JDBC──▶  MySQL
(port 4200)                  (port 8080)                          (hotel_booking)
```

- **Frontend (Angular 20).** Four pages: search, hotel details + booking, confirmation, manage booking. One `ApiService` wraps all HTTP calls. In development `proxy.conf.json` forwards `/api` to the backend, so the browser sees one origin.
- **Backend (Spring Boot).** Layered: `web` (controllers, exception handler) → `service` (business rules) → `repository` (Spring Data JPA) → `domain` (entities). DTOs are Java records, so entities are never exposed directly.
- **Database (MySQL 8).** The schema is owned by Flyway migrations (`V1__create_schema.sql`, `V2__sample_hotels.sql`). Hibernate does not generate tables (`ddl-auto: none`).
- **Business rules live in small pure classes** (`PriceCalculator`, `AvailabilityCalculator`, `StayPolicy`) so they can be unit-tested without a database.

## 2. Database design

| Table | Purpose | Key constraints |
| --- | --- | --- |
| `hotels` | name, location, description, amenities, `price_per_night`, `total_rooms` | price > 0, rooms > 0; index on `location` |
| `room_inventory` | rooms already booked per hotel per night (`booked_rooms`) | FK to hotels, **UNIQUE(hotel_id, stay_date)**, `booked_rooms >= 0` |
| `bookings` | reference, guest name/email, dates, nights, price snapshot, total, status | UNIQUE(reference), `check_out > check_in`, FK to hotels |

Relationships: one hotel has many `room_inventory` rows and many `bookings`.

Notes:
- `room_inventory` rows are created lazily the first time a night is booked, so no job is needed to pre-generate dates. Availability for a night is `hotels.total_rooms - booked_rooms` (a missing row means zero booked).
- `bookings` stores a **price snapshot** (`price_per_night`) so a later price change never alters an existing booking.
- Emails are stored lower-case so lookups are case-insensitive.
- Status is `CONFIRMED` or `CANCELLED`; cancelled bookings are kept for history.

## 3. API design

| Method | Path | Purpose | Success |
| --- | --- | --- | --- |
| GET | `/api/hotels?location&checkIn&checkOut` | list/search. With dates each hotel includes `availableRooms`, `available`, `nights`, `totalAmount` | 200 |
| GET | `/api/hotels/{id}?checkIn&checkOut` | one hotel with the same stay fields | 200 |
| GET | `/api/hotels/{id}/alternatives?checkIn&checkOut` | surprise feature | 200 |
| POST | `/api/bookings` | create booking `{hotelId, checkIn, checkOut, guestName, guestEmail}` | 201 |
| POST | `/api/bookings/lookup` | `{reference, email}` → booking | 200 |
| POST | `/api/bookings/cancel` | `{reference, email}` → cancelled booking | 200 |

Lookup and cancel use POST with a JSON body so the email address does not appear in URLs or access logs.

**Consistent errors.** Every failure returns the same JSON shape:

```json
{ "timestamp": "...", "status": 409, "code": "ROOMS_UNAVAILABLE",
  "message": "Sorry, Lakeview Palace Boutique has no rooms left for the night of 2026-12-20. Please choose different dates.",
  "path": "/api/bookings", "fieldErrors": [] }
```

| Code | HTTP | When |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | bean validation failed (`fieldErrors` lists each field) |
| `INVALID_STAY` | 400 | past check-in, check-out not after check-in, missing dates, stay over 30 nights |
| `MALFORMED_REQUEST` / `INVALID_PARAMETER` / `MISSING_PARAMETER` | 400 | unreadable JSON, bad date format, missing parameter |
| `HOTEL_NOT_FOUND`, `BOOKING_NOT_FOUND` | 404 | unknown id, or reference/email mismatch |
| `ROOMS_UNAVAILABLE` | 409 | no room left for at least one night |
| `ALREADY_CANCELLED`, `CANCELLATION_NOT_ALLOWED` | 409 | cancelling twice, or after the stay started |
| `INTERNAL_ERROR` | 500 | unexpected failure; details are logged, not returned |

## 4. Booking logic

- **Nights and pricing.** `nights = check-out − check-in` (check-out is the departure day and is not a night). `total = pricePerNight × nights`, computed only on the backend with `BigDecimal` (2 decimals). The client's total is never accepted; the booking request does not even contain one.
- **Date validation (`StayPolicy`).** Both dates required; check-in not before today; check-out strictly after check-in; at most 30 nights. "Today" comes from an injected `Clock` in the configured timezone (default `Asia/Kolkata`).
- **Availability.** A stay uses one room on every night in `[check-in, check-out)`. Rooms available for a stay = `totalRooms − max(bookedRooms over those nights)`. The booking is rejected if any night is already full.
- **Overlapping stays (assumption).** Two stays overlap only if they share at least one night. A guest checking out on the 10th does **not** block another guest checking in on the 10th. A long stay that includes one full night is rejected as a whole, and nothing is reserved (all nights are checked before any is changed).
- **Cancellation.** Requires reference + email. Each night's `booked_rooms` is decremented and the booking is marked `CANCELLED`. Cancelling twice returns 409 and releases nothing twice. Assumption: a stay that has already started (check-in before today) cannot be cancelled online.
- **Reference.** `HB-` plus 8 random characters from an unambiguous alphabet (no 0/O/1/I), using `SecureRandom`, checked for uniqueness (plus a DB unique constraint).

## 5. Design decisions, alternatives, trade-offs

| Decision | Alternative considered | Why |
| --- | --- | --- |
| Per-night counter table (`room_inventory`) | Count overlapping bookings with a date-range query on `bookings` | Per-night counters make "is there a room every night" a simple check, give the lock something to protect, and make cancellation a plain decrement. Trade-off: one more table to keep consistent (done in the same transaction). |
| Single `total_rooms` per hotel, no `rooms` table | A row per physical room | The assignment allows one room category; a counter is simpler. Trade-off: no room numbers. |
| Flyway migrations, `ddl-auto: none` | Hibernate auto-create | Reviewable, repeatable schema and sample data, same scripts for MySQL and test H2. |
| Plain constructors and getters, records for DTOs | Lombok | No annotation-processor setup; easy to read. |
| `POST /bookings/lookup` and `/cancel` | `GET /bookings/{ref}?email=` | Keeps email out of URLs and logs. |
| Same "not found" answer for wrong email and unknown reference | Separate messages | Does not reveal whether a reference exists. |
| Amenities as one comma-separated column | Separate table | They are only displayed, never queried. Trade-off: not filterable. |
| URL query parameters hold the search dates in the Angular app | Component state/service | Refresh, back button and sharing links all work. |
| Currency is INR, shown with the `currency` pipe | Multi-currency | Sample data is Indian cities. |

## 6. Simultaneous bookings (two users, last room)

`BookingService.create` runs in one transaction and **first loads the hotel row with `SELECT … FOR UPDATE`** (`PESSIMISTIC_WRITE`). Only one transaction per hotel can hold that lock, so concurrent bookings for the same hotel are processed one after another:

1. Transaction A locks the hotel, sees 1 room left, increments the counters, saves the booking, commits.
2. Transaction B was waiting on the lock. When it gets it, it reads the updated counters, finds the night full and fails with `ROOMS_UNAVAILABLE` (HTTP 409). The UI then reloads availability and shows nearby dates.

Both operations run with `READ_COMMITTED` isolation. MySQL's default (`REPEATABLE READ`) could otherwise keep showing a snapshot taken before the lock was granted and hide the other guest's booking.

Cancellation takes the same lock first, so the lock order is always hotel → inventory/bookings and deadlocks cannot occur. The `UNIQUE(hotel_id, stay_date)` constraint and `booked_rooms >= 0` check are a second line of defence.

`BookingServiceIntegrationTest.twoGuestsRacingForTheLastRoomOnlyOneWins` starts 8 threads at once for the last room and asserts that exactly one succeeds.

Alternatives: optimistic locking with a `@Version` column and retry (better under low contention, needs retry logic); a conditional update `UPDATE room_inventory SET booked_rooms = booked_rooms + 1 WHERE … AND booked_rooms < :total` per night (no explicit lock, but multi-night stays need care to roll back). Locking per hotel is the simplest correct choice here. Trade-off: bookings for the same hotel are serialised, which is fine at this scale but would need per-hotel-per-date locking or a different approach for very high traffic.

## 7. Surprise feature: smart date suggestions

- **What it does.** If the chosen dates are sold out, the hotel page lists up to three nearby stays of the same length (up to 14 days earlier or later) that still have rooms, closest first, with the total price and a "Use these dates" button that re-runs the availability check. The search results also change the button to "See nearby dates" for sold-out hotels.
- **Why.** The most frustrating moment in booking is "sold out" followed by trial and error. A small, concrete suggestion turns a dead end into a booking.
- **How.** `GET /api/hotels/{id}/alternatives` → `AlternativeDatesService`. It loads the hotel's inventory for the whole ±14 day window in one query, then tries shifts +1, −1, +2, −2 … using the same `AvailabilityCalculator` as normal bookings, skipping any window that starts in the past. The Angular page calls it whenever `available === false`, and also after a booking fails with `ROOMS_UNAVAILABLE`.

## 8. Limitations and improvements

Not done / known limitations:
- No authentication, payments, admin panel or emails (all out of scope). The booking reference and email act as the only credential.
- One room category per hotel; one room per booking.
- The backend tests are written for H2 in MySQL mode. They have not been run against a real MySQL instance in CI.
- No browser end-to-end tests; the UI was exercised manually.
- Anyone who guesses a reference and email could cancel; there is no rate limiting on lookup/cancel.
- Locking is per hotel, which serialises bookings of the same hotel.

With more time:
- Rate limiting and CAPTCHA on lookup/cancel; send a confirmation email with the reference.
- Room types and per-date pricing (weekend rates); date-range pricing in `room_inventory`.
- Testcontainers-based tests against real MySQL; Playwright end-to-end tests.
- Pagination and filters (price, amenities); hotel images.
- OpenAPI documentation (springdoc) and a Dockerfile / docker-compose for one-command startup.
