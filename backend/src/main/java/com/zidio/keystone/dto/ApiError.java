package com.zidio.keystone.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String message,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}
}
