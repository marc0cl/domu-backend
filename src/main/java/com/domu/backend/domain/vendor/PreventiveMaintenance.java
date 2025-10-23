package com.domu.backend.domain.vendor;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDate;
import java.util.Objects;

public record PreventiveMaintenance(
        Long id,
        Long providerId,
        String description,
        LocalDate scheduledDate,
        String periodicity,
        String status
) implements Identifiable<PreventiveMaintenance> {
    public PreventiveMaintenance {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(scheduledDate, "scheduledDate");
    }

    @Override
    public PreventiveMaintenance withId(Long newId) {
        return new PreventiveMaintenance(newId, providerId, description, scheduledDate, periodicity, status);
    }
}
