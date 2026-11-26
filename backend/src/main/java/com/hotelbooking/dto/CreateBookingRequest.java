package com.hotelbooking.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
        @NotNull(message = "Hotel is required.")
        Long hotelId,

        @NotNull(message = "Check-in date is required.")
        LocalDate checkIn,

        @NotNull(message = "Check-out date is required.")
        LocalDate checkOut,

        @NotBlank(message = "Guest name is required.")
        @Size(min = 2, max = 100, message = "Guest name must be between 2 and 100 characters.")
        String guestName,

        @NotBlank(message = "Email address is required.")
        @Email(message = "Please enter a valid email address.")
        @Size(max = 254, message = "Email address is too long.")
        String guestEmail) {
}
