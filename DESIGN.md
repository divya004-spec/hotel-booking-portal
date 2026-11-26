# Design

## 1. Architecture

Three layers. The Angular app (port 4200) shows pages and calls a JSON REST API under `/api`. In development a proxy forwards those calls to Spring Boot (port 8080); CORS is also configured. Spring Boot is layered as controller (HTTP only), service (all rules) and repository (database access). MySQL stores the data; Flyway creates the schema from versioned SQL files.

The browser is never trusted: the backend recalculates price and re-checks dates and availability on every booking.

## 2. Database design

- `hotels`: name, location, description, amenities (comma separated), price_per_night, total_rooms.
- `room_inventory`: one row per hotel per night (hotel_id, stay_date, total_rooms, booked_rooms). Unique on (hotel_id, stay_date). CHECK that booked_rooms is between 0 and total_rooms.
- `bookings`: unique reference, hotel, guest name and email, check_in, check_out, nights, price_per_night (copied, so later price changes do not alter old bookings), total_amount, status (CONFIRMED or CANCELLED), timestamps. CHECK that check_out is after check_in.

A hotel has many inventory rows and many bookings. A booking uses one inventory row for each night of its stay.

## 3. API design

| Method and path | Purpose |
| --- | --- |
| GET /api/hotels?location=&checkIn=&checkOut= | Browse or search; with dates each hotel includes nights, rooms left and total |
| GET /api/hotels/locations | City names for search suggestions |
| GET /api/hotels/{id}?checkIn=&checkOut= | Hotel details and quote |
| GET /api/hotels/{id}/alternatives?checkIn=&checkOut= | Surprise feature |
| POST /api/bookings | Create a booking (201) |
| GET /api/bookings/{reference}?email= | Retrieve a booking |
| POST /api/bookings/{reference}/cancel | Cancel (body: email) |

Every error uses one JSON shape: timestamp, status, error, code, message, path, fieldErrors. Codes include VALIDATION_ERROR (400), NOT_FOUND (404), NO_AVAILABILITY and ALREADY_CANCELLED (409), INTERNAL_ERROR (500).

## 4. Booking logic

- **Dates:** check-in not in the past, check-out after check-in, at most 30 nights.
- **Price:** nights x price per night, computed in PricingService with BigDecimal, 2 decimals.
- **Availability:** a stay needs a free room on every night; availability is the minimum free rooms across its nights.
- **Create:** one transaction: lock the stay's inventory rows, check, increment booked_rooms on each night, save the booking.
- **Cancel:** needs reference and matching email; marks CANCELLED and decrements booked_rooms on every night. Cancelling twice is refused.
- **Overlaps:** stays are half-open ranges [check-in, check-out), so same-day turnover is allowed while any shared night counts against capacity.

## 5. Design decisions

| Decision | Alternative considered | Trade-off |
| --- | --- | --- |
| Per-night inventory rows | Count overlapping bookings on each request | Rows make locking and "minimum across nights" simple and fast; cost is a nightly top-up job and more rows |
| Flyway SQL migrations | Hibernate auto-creating tables | Reviewable, repeatable schema; slightly more setup |
| DTO records, entities hidden | Returning entities | Stable API, no lazy-loading leaks |
| Amenities as comma-separated text | Separate amenities table | Much simpler for the assignment; harder to query by amenity |
| Lookup by reference + email | Login accounts | Meets scope; email is a weak secret |
| Same message for wrong reference or wrong email | Distinct messages | Avoids revealing which references exist |

## 6. Simultaneous bookings

Two users booking the last room send two requests at once. Each runs in a transaction that selects the stay's inventory rows with SELECT ... FOR UPDATE (pessimistic write lock), always in date order to avoid deadlocks. The second request waits until the first commits, then sees booked_rooms equal to total_rooms and receives 409 NO_AVAILABILITY. As a last line of defence the database CHECK constraint rejects any overbooking, and that error is also mapped to a 409. An automated test starts 6 threads for 2 rooms and asserts exactly 2 succeed.

Could be improved with optimistic locking (a version column plus retry, better under low contention), or a hold-then-confirm flow with expiring reservations.

## 7. Surprise feature: nearby free dates

**Purpose:** a sold-out message is a dead end. The page instead offers the closest free stays of the same length so the guest can still book.

**Implementation:** AvailabilityService.findAlternatives loads the inventory for the requested window plus or minus 14 days in one query, slides a window of the same number of nights across it, keeps stays whose every night has a free room, sorts by distance from the requested check-in (earlier first on ties) and returns up to 3. The Angular hotel page calls /alternatives only when the quote says sold out; clicking an option updates the URL and re-quotes.

## 8. Limitations and improvements

- No authentication or email verification; no payment; no admin.
- Search is by city text only; no pagination or sorting by price.
- One room category and one room per booking.
- Time zone is the server's; hotels in other zones would need per-hotel zones.
- Tests run on H2, not MySQL. With more time: Testcontainers with real MySQL, frontend unit and end-to-end tests, rate limiting on lookup and cancel, an audit trail, and holding a room for a few minutes during checkout.
