package com.domu.domain.staff;

import java.time.LocalDate;
import java.util.Objects;

public record Personnel(
        Long id,
        Long buildingId,
        String firstName,
        String lastName,
        String documentNumber,
        String jobTitle,
        String phone,
        String email,
        LocalDate contractDate,
        Boolean active
) {
    public Personnel {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(jobTitle, "jobTitle");
    }
}
