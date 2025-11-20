package com.domu.domain.access;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record Visit(
        Long id,
        String visitorName,
        String documentNumber,
        String licensePlate,
        String purpose,
        LocalDate visitDate,
        LocalTime startTime,
        LocalTime endTime,
        String status
) {
    public Visit {
        Objects.requireNonNull(visitorName, "visitorName");
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(visitDate, "visitDate");
    }
}
