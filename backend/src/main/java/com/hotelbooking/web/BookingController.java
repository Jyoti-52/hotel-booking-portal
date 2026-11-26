package com.hotelbooking.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hotelbooking.dto.BookingLookupRequest;
import com.hotelbooking.dto.BookingResponse;
import com.hotelbooking.dto.CreateBookingRequest;
import com.hotelbooking.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(request);
    }

    /**
     * POST (not GET) so the guest's email address is sent in the body and does not end up in URLs and server logs.
     */
    @PostMapping("/lookup")
    public BookingResponse lookup(@Valid @RequestBody BookingLookupRequest request) {
        return bookingService.find(request.reference(), request.email());
    }

    @PostMapping("/cancel")
    public BookingResponse cancel(@Valid @RequestBody BookingLookupRequest request) {
        return bookingService.cancel(request.reference(), request.email());
    }
}
