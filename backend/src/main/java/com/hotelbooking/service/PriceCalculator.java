package com.hotelbooking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Pure pricing rules. The total is always computed here on the backend, never trusted from the client. */
public final class PriceCalculator {

    private PriceCalculator() {
    }

    /** Number of nights between check-in (inclusive) and check-out (exclusive). */
    public static int nights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** total = pricePerNight x nights, with two decimal places. */
    public static BigDecimal total(BigDecimal pricePerNight, int nights) {
        return pricePerNight.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
    }
}
