package com.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AvailabilityCalculatorTest {

    private static final LocalDate D1 = LocalDate.of(2026, 12, 1);

    @Test
    void allRoomsAvailableWhenNothingIsBooked() {
        assertEquals(5, AvailabilityCalculator.availableRooms(5, Map.of(), D1, D1.plusDays(3)));
    }

    @Test
    void availabilityIsLimitedByTheBusiestNight() {
        Map<LocalDate, Integer> booked = Map.of(D1, 1, D1.plusDays(1), 4, D1.plusDays(2), 2);
        assertEquals(1, AvailabilityCalculator.availableRooms(5, booked, D1, D1.plusDays(3)));
    }

    @Test
    void soldOutNightMeansNoRoomsForTheWholeStay() {
        Map<LocalDate, Integer> booked = Map.of(D1.plusDays(1), 2);
        assertEquals(0, AvailabilityCalculator.availableRooms(2, booked, D1, D1.plusDays(3)));
    }

    @Test
    void checkOutDayIsNotCounted() {
        // The only booking is on the night of Dec 3, which is the check-out day of a Dec 1-3 stay.
        Map<LocalDate, Integer> booked = Map.of(D1.plusDays(2), 2);
        assertEquals(2, AvailabilityCalculator.availableRooms(2, booked, D1, D1.plusDays(2)));
    }

    @Test
    void neverReturnsNegative() {
        Map<LocalDate, Integer> booked = Map.of(D1, 9);
        assertEquals(0, AvailabilityCalculator.availableRooms(2, booked, D1, D1.plusDays(1)));
    }
}
