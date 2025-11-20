package com.domu.domain.vendor;

import java.time.LocalDate;
import java.util.Objects;

public record PreventiveMaintenance(
        Long id,
        Long providerId,
        String description,
        LocalDate scheduledDate,
        String periodicity,
        String status
) {
    public PreventiveMaintenance {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(scheduledDate, "scheduledDate");
    }
}
