package com.domu.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record VisitRequest(
        @NotNull Long communityId,
        @NotNull Long residentId,
        @NotBlank String visitorName,
        String visitorRut,
        String reason,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        boolean preauthorized,
        String qrCode
) {
}
