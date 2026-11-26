package com.hotelbooking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A nearby stay of the same length that still has rooms. */
public record AlternativeStayResponse(
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        int shiftDays,
        int availableRooms,
        BigDecimal totalAmount) {
}
