package com.domu.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull Long amenityId,
        @NotNull Long residentId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        Integer attendeesCount,
        String status
) {
}
