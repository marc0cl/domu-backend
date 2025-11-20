package com.domu.backend.domain.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public record User(
        Long id,
        Long unitId,
        Long roleId,
        String firstName,
        String lastName,
        String email,|
        String phone,
        LocalDate birthDate,
        String passwordHash,
        String documentNumber,
        Boolean resident,
        LocalDateTime createdAt,
        String status
) {
    public User {
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public User withId(Long newId) {
        return new User(
                newId,
                unitId,
                roleId,
                firstName,
                lastName,
                email,
                phone,
                birthDate,
                passwordHash,
                documentNumber,
                resident,
                createdAt,
                status
        );
    }
}
