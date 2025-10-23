package com.domu.backend.domain.access;

import com.domu.backend.domain.Identifiable;

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
) implements Identifiable<Visit> {
    public Visit {
        Objects.requireNonNull(visitorName, "visitorName");
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(visitDate, "visitDate");
    }

    @Override
    public Visit withId(Long newId) {
        return new Visit(newId, visitorName, documentNumber, licensePlate, purpose, visitDate, startTime, endTime, status);
    }
}
