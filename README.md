# Stayfinder: Hotel Booking Portal

A small full-stack hotel booking portal built for the Thomas Cook / TC Tours developer internship assessment.

- **Backend:** Java 21 with Spring Boot 3.3, Spring Data JPA, Flyway
- **Frontend:** Angular 20 (standalone components, signals)
- **Database:** MySQL 8
- **Surprise feature:** *Smart date suggestions*. When your dates are sold out, the app suggests the nearest dates that still have rooms, with a one-click "Use these dates".

Design write-up: see [DESIGN.md](DESIGN.md).

## What you can do

1. Browse hotels (name, location, description, amenities, price per night). Seven sample hotels are loaded automatically.
2. Search by location and choose check-in and check-out dates.
3. Open a hotel to see the rooms left for your dates and the total price (calculated by the backend).
4. Book one room with your name and email, and receive a unique booking reference (for example `HB-7K3M9QXD`).
5. Look up a booking with its reference and email, and cancel it. Cancelling releases the rooms.

## Prerequisites

| Tool | Version |
| --- | --- |
| JDK | 21 |
| Maven | 3.9+ |
| Node.js | 20.19+ or 22 |
| MySQL | 8.0+ |

## Run it locally

### 1. Create the database

Edit `database/setup.sql` and replace `CHANGE_ME` with a password of your choice, then:

```bash
mysql -u root -p < database/setup.sql
```

This creates the `hotel_booking` database and a `hotel_app` user. The tables and sample data are created automatically by Flyway migrations when the backend starts (`backend/src/main/resources/db/migration`).

### 2. Start the backend

Credentials come from environment variables. Nothing secret is stored in the repository (see `.env.example`).

```bash
cd backend
export DB_PASSWORD='the-password-you-chose'      # Windows PowerShell: $env:DB_PASSWORD='...'
mvn spring-boot:run
```

The API is now on http://localhost:8080 (try http://localhost:8080/api/hotels).

Optional variables: `DB_URL`, `DB_USERNAME` (default `hotel_app`), `APP_TIMEZONE` (default `Asia/Kolkata`), `CORS_ALLOWED_ORIGINS`, `SERVER_PORT`.

### 3. Start the frontend

```bash
cd frontend
npm install
npm start
```

Open http://localhost:4200.

## Run the tests

```bash
cd backend
mvn test
```

Tests use an in-memory H2 database in MySQL mode and run the same Flyway migrations, so no MySQL is needed for them.

| Test class | What it covers |
| --- | --- |
| `PriceCalculatorTest` | nights and total amount |
| `StayPolicyTest` | past check-in, check-out not after check-in, missing dates, maximum stay |
| `AvailabilityCalculatorTest` | tightest-night rule, sold-out nights, check-out day not counted |
| `BookingServiceIntegrationTest` | booking, capacity limit, overlapping and back-to-back stays, cancellation releases rooms, double cancel, email check, **8 threads racing for the last room**, date suggestions |
| `ApiErrorResponseTest` | consistent JSON error shape for validation errors, bad dates, unknown hotel |

## API summary

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/hotels?location=&checkIn=&checkOut=` | list/search hotels; with dates also returns rooms left and total |
| GET | `/api/hotels/{id}?checkIn=&checkOut=` | hotel details and availability |
| GET | `/api/hotels/{id}/alternatives?checkIn=&checkOut=` | surprise feature: nearby dates with rooms |
| POST | `/api/bookings` | create a booking |
| POST | `/api/bookings/lookup` | find a booking by reference + email |
| POST | `/api/bookings/cancel` | cancel a booking by reference + email |

Full details are in [DESIGN.md](DESIGN.md).

## Screenshots / demo

Add your screenshots or screen recording of the main flow to `docs/screenshots/` and list them here before submitting.

## Time spent and unfinished items

- **Approximate time spent:** _fill in your own hours here before submitting._
- **Unfinished / known limitations:** see section 8 of [DESIGN.md](DESIGN.md). In short: no authentication, no payments, no emails are sent, one room category per hotel, and no browser end-to-end tests.

## AI assistance disclosure

I used an AI assistant (Claude by Anthropic) to help scaffold and write significant parts of this project, including the backend, the Angular frontend, the tests and the documentation. I reviewed the code, and I can explain the design and the code in the interview. _Edit this paragraph so it accurately reflects how you used the assistant and what you changed yourself._
