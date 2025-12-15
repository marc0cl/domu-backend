package com.domu.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        Long unitId,
        Long roleId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String email,
        String phone,
        String documentNumber,
        Boolean resident,
        LocalDateTime createdAt,
        String status,
        Long activeBuildingId,
        java.util.List<BuildingSummaryResponse> buildings
) {
}
