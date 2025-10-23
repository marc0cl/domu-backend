package com.domu.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ParkingPermitRequest(
        @NotNull Long parkingSpaceId,
        Long residentId,
        Long visitId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        String permitType
) {
}
