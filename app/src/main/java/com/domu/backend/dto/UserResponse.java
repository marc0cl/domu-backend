package com.domu.backend.dto;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        Long unitId,
        Long roleId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String email
) {
}
