package com.hotelbooking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Hotel as shown to the user. The stay fields (checkIn ... available) are only present
 * when the request included a date range.
 */
public record HotelResponse(
        Long id,
        String name,
        String location,
        String description,
        List<String> amenities,
        BigDecimal pricePerNight,
        int totalRooms,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer nights,
        BigDecimal totalAmount,
        Integer availableRooms,
        Boolean available,
        BigDecimal rating,
        Integer reviewCount,
        String imageUrl) {
}
