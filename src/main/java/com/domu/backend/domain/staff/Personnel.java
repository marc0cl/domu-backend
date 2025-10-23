package com.domu.backend.domain.staff;

import com.domu.backend.domain.Identifiable;

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
) implements Identifiable<Personnel> {
    public Personnel {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(jobTitle, "jobTitle");
    }

    @Override
    public Personnel withId(Long newId) {
        return new Personnel(newId, buildingId, firstName, lastName, documentNumber, jobTitle, phone, email, contractDate, active);
    }
}
