package com.domu.backend.domain;

import java.time.LocalDate;

public record User(
        Long id,
        Long unitId,
        Long roleId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String email,
        String passwordHash
) {
    public User withId(Long newId) {
        return new User(newId, unitId, roleId, firstName, lastName, birthDate, email, passwordHash);
    }
}
