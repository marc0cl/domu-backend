package com.domu.backend.dto;

import java.time.Instant;

public record ErrorResponse(String message, int status, Instant timestamp) {
    public static ErrorResponse of(String message, int status) {
        return new ErrorResponse(message, status, Instant.now());
    }
}
