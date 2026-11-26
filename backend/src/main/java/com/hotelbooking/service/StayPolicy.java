package com.hotelbooking.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.hotelbooking.exception.ApiException;
import com.hotelbooking.exception.ErrorCode;

/** Date rules for a stay. */
@Component
public class StayPolicy {

    public static final int MAX_NIGHTS = 30;

    private final Clock clock;

    public StayPolicy(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public void validate(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null) {
            throw invalid("Check-in date is required.");
        }
        if (checkOut == null) {
            throw invalid("Check-out date is required.");
        }
        if (checkIn.isBefore(today())) {
            throw invalid("Check-in date cannot be in the past.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("Check-out date must be after the check-in date.");
        }
        if (PriceCalculator.nights(checkIn, checkOut) > MAX_NIGHTS) {
            throw invalid("A single booking cannot be longer than " + MAX_NIGHTS + " nights.");
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(ErrorCode.INVALID_STAY, message);
    }
}
