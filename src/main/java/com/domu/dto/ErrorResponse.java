package com.domu.dto;

import java.time.Instant;

public record ErrorResponse(String message, Integer status, Instant timestamp) {
    public static ErrorResponse of(String message, Integer status) {
        return new ErrorResponse(message, status, Instant.now());
    }
}
