package com.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.hotelbooking.exception.ApiException;
import com.hotelbooking.exception.ErrorCode;

class StayPolicyTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    // "Today" is fixed at 2026-12-10 so the tests never depend on the real date.
    private final Clock clock = Clock.fixed(Instant.parse("2026-12-10T06:00:00Z"), ZONE);
    private final StayPolicy policy = new StayPolicy(clock);

    private static final LocalDate TODAY = LocalDate.of(2026, 12, 10);

    @Test
    void acceptsCheckInToday() {
        assertDoesNotThrow(() -> policy.validate(TODAY, TODAY.plusDays(1)));
    }

    @Test
    void rejectsCheckInInThePast() {
        ApiException ex = assertThrows(ApiException.class,
                () -> policy.validate(TODAY.minusDays(1), TODAY.plusDays(2)));
        assertEquals(ErrorCode.INVALID_STAY, ex.getCode());
        assertEquals("Check-in date cannot be in the past.", ex.getMessage());
    }

    @Test
    void rejectsCheckOutOnTheSameDayAsCheckIn() {
        ApiException ex = assertThrows(ApiException.class,
                () -> policy.validate(TODAY.plusDays(3), TODAY.plusDays(3)));
        assertEquals("Check-out date must be after the check-in date.", ex.getMessage());
    }

    @Test
    void rejectsCheckOutBeforeCheckIn() {
        assertThrows(ApiException.class, () -> policy.validate(TODAY.plusDays(5), TODAY.plusDays(2)));
    }

    @Test
    void rejectsMissingDates() {
        assertThrows(ApiException.class, () -> policy.validate(null, TODAY.plusDays(2)));
        assertThrows(ApiException.class, () -> policy.validate(TODAY, null));
    }

    @Test
    void rejectsStaysLongerThanTheMaximum() {
        assertDoesNotThrow(() -> policy.validate(TODAY, TODAY.plusDays(StayPolicy.MAX_NIGHTS)));
        assertThrows(ApiException.class, () -> policy.validate(TODAY, TODAY.plusDays(StayPolicy.MAX_NIGHTS + 1)));
    }
}
