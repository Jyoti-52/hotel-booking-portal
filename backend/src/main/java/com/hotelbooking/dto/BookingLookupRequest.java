package com.hotelbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Reference + email together identify a booking (used for both lookup and cancellation). */
public record BookingLookupRequest(
        @NotBlank(message = "Booking reference is required.")
        String reference,

        @NotBlank(message = "Email address is required.")
        @Email(message = "Please enter a valid email address.")
        String email) {
}
