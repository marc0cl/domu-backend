package com.domu.domain.core;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record User(
        Long id,
        Long unitId,
        Long roleId,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate birthDate,
        String passwordHash,
        String documentNumber,
        Boolean resident,
        LocalDateTime createdAt,
        String status,
        String bio,
        String avatarBoxId,
        String privacyAvatarBoxId
) {
    public User {
        // Validaciones mínimas para permitir el inicio de sesión incluso con datos incompletos
        if (email == null) email = "unknown@domu.app";
        if (firstName == null) firstName = "Usuario";
        if (lastName == null) lastName = "DOMU";
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
                status,
                bio,
                avatarBoxId,
                privacyAvatarBoxId
        );
    }
}