package com.hotelbooking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hotelbooking.domain.BookingStatus;

public record BookingResponse(
        String reference,
        BookingStatus status,
        Long hotelId,
        String hotelName,
        String hotelLocation,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal pricePerNight,
        BigDecimal totalAmount,
        String guestName,
        String guestEmail,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt,
        boolean cancellable) {
}
