package com.hotelbooking.dto;

import java.time.Instant;
import java.util.List;

/** The single error shape returned by every failing API call. */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldIssue> fieldErrors) {

    public record FieldIssue(String field, String message) {
    }
}
