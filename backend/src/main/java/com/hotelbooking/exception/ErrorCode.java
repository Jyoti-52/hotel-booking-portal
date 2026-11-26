package com.hotelbooking.exception;

import org.springframework.http.HttpStatus;

/** Machine-readable error codes returned in every API error body. */
public enum ErrorCode {
    INVALID_STAY(HttpStatus.BAD_REQUEST),
    HOTEL_NOT_FOUND(HttpStatus.NOT_FOUND),
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND),
    ROOMS_UNAVAILABLE(HttpStatus.CONFLICT),
    ALREADY_CANCELLED(HttpStatus.CONFLICT),
    CANCELLATION_NOT_ALLOWED(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
