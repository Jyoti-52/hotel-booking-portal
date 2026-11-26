package com.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

    @Test
    void countsNightsBetweenCheckInAndCheckOut() {
        assertEquals(1, PriceCalculator.nights(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2)));
        assertEquals(3, PriceCalculator.nights(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4)));
    }

    @Test
    void countsNightsAcrossMonthAndYearBoundaries() {
        assertEquals(4, PriceCalculator.nights(LocalDate.of(2026, 12, 29), LocalDate.of(2027, 1, 2)));
    }

    @Test
    void totalIsPricePerNightTimesNights() {
        BigDecimal total = PriceCalculator.total(new BigDecimal("6500.00"), 3);
        assertEquals(new BigDecimal("19500.00"), total);
    }

    @Test
    void totalKeepsTwoDecimalPlaces() {
        BigDecimal total = PriceCalculator.total(new BigDecimal("1999.99"), 2);
        assertEquals(new BigDecimal("3999.98"), total);
    }
}
