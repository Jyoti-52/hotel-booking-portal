package com.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hotelbooking.domain.BookingStatus;
import com.hotelbooking.domain.Hotel;
import com.hotelbooking.dto.AlternativeStayResponse;
import com.hotelbooking.dto.BookingResponse;
import com.hotelbooking.dto.CreateBookingRequest;
import com.hotelbooking.dto.HotelResponse;
import com.hotelbooking.exception.ApiException;
import com.hotelbooking.exception.ErrorCode;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomInventoryRepository;

/** Runs the real services against an in-memory database (same Flyway migrations as production). */
@SpringBootTest
class BookingServiceIntegrationTest {

    @Autowired BookingService bookingService;
    @Autowired HotelService hotelService;
    @Autowired AlternativeDatesService alternativeDatesService;
    @Autowired HotelRepository hotelRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired RoomInventoryRepository inventoryRepository;
    @Autowired Clock clock;

    private LocalDate today;
    private Hotel hotel; // a fresh hotel with exactly 2 rooms at 1000.00 per night

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        inventoryRepository.deleteAll();
        // remove hotels created by earlier tests (the 7 sample hotels are not in "Testville")
        hotelRepository.deleteAll(hotelRepository.findByLocationContainingIgnoreCaseOrderByNameAsc("Testville"));
        today = LocalDate.now(clock);
        hotel = hotelRepository.save(new Hotel("Test Hotel " + System.nanoTime(), "Testville",
                "A hotel used by automated tests.", "WiFi", new BigDecimal("1000.00"), 2));
    }

    private CreateBookingRequest request(LocalDate in, LocalDate out, String name, String email) {
        return new CreateBookingRequest(hotel.getId(), in, out, name, email);
    }

    private int available(LocalDate in, LocalDate out) {
        HotelResponse r = hotelService.getHotel(hotel.getId(), in, out);
        return r.availableRooms();
    }

    @Test
    void bookingCalculatesTotalOnTheBackendAndReturnsReference() {
        LocalDate in = today.plusDays(10);
        BookingResponse booking = bookingService.create(request(in, in.plusDays(3), "Asha Rao", "Asha@Example.com"));

        assertNotNull(booking.reference());
        assertTrue(booking.reference().startsWith("HB-"));
        assertEquals(3, booking.nights());
        assertEquals(new BigDecimal("3000.00"), booking.totalAmount());
        assertEquals(BookingStatus.CONFIRMED, booking.status());
        assertEquals("asha@example.com", booking.guestEmail());
        assertEquals(1, available(in, in.plusDays(3)));
    }

    @Test
    void lastRoomCanBeBookedButNotMoreThanCapacity() {
        LocalDate in = today.plusDays(10);
        bookingService.create(request(in, in.plusDays(2), "Guest One", "one@example.com"));
        bookingService.create(request(in, in.plusDays(2), "Guest Two", "two@example.com"));

        ApiException ex = assertThrows(ApiException.class,
                () -> bookingService.create(request(in, in.plusDays(2), "Guest Three", "three@example.com")));
        assertEquals(ErrorCode.ROOMS_UNAVAILABLE, ex.getCode());
        assertEquals(0, available(in, in.plusDays(2)));
    }

    @Test
    void overlappingStayIsRejectedWhenAnyNightIsFull() {
        LocalDate in = today.plusDays(10);
        // Two rooms taken for the night of day 11 only.
        bookingService.create(request(in.plusDays(1), in.plusDays(2), "Guest One", "one@example.com"));
        bookingService.create(request(in.plusDays(1), in.plusDays(2), "Guest Two", "two@example.com"));

        // A longer stay that includes that night must fail, and must not reserve any other night.
        ApiException ex = assertThrows(ApiException.class,
                () -> bookingService.create(request(in, in.plusDays(3), "Guest Three", "three@example.com")));
        assertEquals(ErrorCode.ROOMS_UNAVAILABLE, ex.getCode());
        assertEquals(2, available(in, in.plusDays(1)));
        assertEquals(2, available(in.plusDays(2), in.plusDays(3)));
    }

    @Test
    void backToBackStaysDoNotOverlap() {
        LocalDate in = today.plusDays(10);
        bookingService.create(request(in, in.plusDays(2), "Guest One", "one@example.com"));
        bookingService.create(request(in, in.plusDays(2), "Guest Two", "two@example.com"));

        // Check-in on the day the others check out is fine.
        BookingResponse next = bookingService.create(
                request(in.plusDays(2), in.plusDays(4), "Guest Three", "three@example.com"));
        assertEquals(BookingStatus.CONFIRMED, next.status());
    }

    @Test
    void cancellingReleasesAvailability() {
        LocalDate in = today.plusDays(10);
        BookingResponse first = bookingService.create(request(in, in.plusDays(2), "Guest One", "one@example.com"));
        bookingService.create(request(in, in.plusDays(2), "Guest Two", "two@example.com"));
        assertEquals(0, available(in, in.plusDays(2)));

        BookingResponse cancelled = bookingService.cancel(first.reference(), "ONE@example.com");

        assertEquals(BookingStatus.CANCELLED, cancelled.status());
        assertEquals(1, available(in, in.plusDays(2)));
        // and the freed room can be booked again
        bookingService.create(request(in, in.plusDays(2), "Guest Three", "three@example.com"));
        assertEquals(0, available(in, in.plusDays(2)));
    }

    @Test
    void cancellingTwiceIsRejected() {
        LocalDate in = today.plusDays(10);
        BookingResponse booking = bookingService.create(request(in, in.plusDays(1), "Guest One", "one@example.com"));
        bookingService.cancel(booking.reference(), "one@example.com");

        ApiException ex = assertThrows(ApiException.class,
                () -> bookingService.cancel(booking.reference(), "one@example.com"));
        assertEquals(ErrorCode.ALREADY_CANCELLED, ex.getCode());
        // a second cancel must not release rooms twice
        assertEquals(2, available(in, in.plusDays(1)));
    }

    @Test
    void lookupAndCancelRequireTheMatchingEmail() {
        LocalDate in = today.plusDays(10);
        BookingResponse booking = bookingService.create(request(in, in.plusDays(1), "Guest One", "one@example.com"));

        ApiException lookup = assertThrows(ApiException.class,
                () -> bookingService.find(booking.reference(), "someone.else@example.com"));
        assertEquals(ErrorCode.BOOKING_NOT_FOUND, lookup.getCode());

        ApiException cancel = assertThrows(ApiException.class,
                () -> bookingService.cancel(booking.reference(), "someone.else@example.com"));
        assertEquals(ErrorCode.BOOKING_NOT_FOUND, cancel.getCode());

        assertEquals(booking.reference(), bookingService.find(booking.reference().toLowerCase(), " ONE@example.com ").reference());
    }

    @Test
    void invalidDatesAreRejectedBeforeAnythingIsReserved() {
        ApiException past = assertThrows(ApiException.class,
                () -> bookingService.create(request(today.minusDays(1), today.plusDays(1), "Guest One", "one@example.com")));
        assertEquals(ErrorCode.INVALID_STAY, past.getCode());

        ApiException inverted = assertThrows(ApiException.class,
                () -> bookingService.create(request(today.plusDays(5), today.plusDays(5), "Guest One", "one@example.com")));
        assertEquals(ErrorCode.INVALID_STAY, inverted.getCode());
        assertEquals(0, bookingRepository.count());
    }

    @Test
    void unknownHotelIsReported() {
        LocalDate in = today.plusDays(10);
        ApiException ex = assertThrows(ApiException.class, () -> bookingService.create(
                new CreateBookingRequest(-1L, in, in.plusDays(1), "Guest One", "one@example.com")));
        assertEquals(ErrorCode.HOTEL_NOT_FOUND, ex.getCode());
    }

    @Test
    void twoGuestsRacingForTheLastRoomOnlyOneWins() throws Exception {
        LocalDate in = today.plusDays(20);
        // Leave exactly one room.
        bookingService.create(request(in, in.plusDays(2), "Guest One", "one@example.com"));

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int n = i;
            Callable<Boolean> task = () -> {
                ready.countDown();
                go.await();
                try {
                    bookingService.create(request(in, in.plusDays(2), "Racer " + n, "racer" + n + "@example.com"));
                    return true;
                } catch (ApiException e) {
                    assertEquals(ErrorCode.ROOMS_UNAVAILABLE, e.getCode());
                    return false;
                }
            };
            results.add(pool.submit(task));
        }
        ready.await();
        go.countDown();

        int successes = 0;
        for (Future<Boolean> f : results) {
            if (f.get(60, TimeUnit.SECONDS)) {
                successes++;
            }
        }
        pool.shutdown();

        assertEquals(1, successes, "exactly one concurrent booking may take the last room");
        assertEquals(0, available(in, in.plusDays(2)));
        // the first booking plus the single winner
        assertEquals(2, bookingRepository.count());
    }

    @Test
    void suggestsNearestDatesWithRoomsLeftWhenSoldOut() {
        LocalDate in = today.plusDays(10);
        // Both rooms are taken for the nights of day +10 and day +11, so the requested stay is sold out.
        bookingService.create(request(in, in.plusDays(2), "Guest One", "one@example.com"));
        bookingService.create(request(in, in.plusDays(2), "Guest Two", "two@example.com"));

        List<AlternativeStayResponse> suggestions = alternativeDatesService.suggest(hotel.getId(), in, in.plusDays(2));

        assertEquals(3, suggestions.size());
        // Shifts of +1 and -1 still touch a full night, so the closest valid shifts are +2, -2 and then +3.
        for (AlternativeStayResponse s : suggestions) {
            assertEquals(2, s.nights());
            assertTrue(s.availableRooms() > 0);
            assertEquals(new BigDecimal("2000.00"), s.totalAmount());
            // no suggestion may overlap the sold-out nights
            assertTrue(!s.checkIn().isBefore(in.plusDays(2)) || !s.checkOut().isAfter(in));
        }
        assertEquals(2, Math.abs(suggestions.get(0).shiftDays()));
    }

    @Test
    void searchShowsAvailabilityAndPriceForTheStay() {
        LocalDate in = today.plusDays(10);
        bookingService.create(request(in, in.plusDays(1), "Guest One", "one@example.com"));

        List<HotelResponse> results = hotelService.search("testville", in, in.plusDays(2));

        assertEquals(1, results.size());
        HotelResponse r = results.get(0);
        assertEquals(1, r.availableRooms());
        assertEquals(2, r.nights());
        assertEquals(new BigDecimal("2000.00"), r.totalAmount());
        assertTrue(r.available());
    }
}
