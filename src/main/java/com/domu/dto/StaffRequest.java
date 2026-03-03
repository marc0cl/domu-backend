package com.domu.dto;

public record StaffRequest(
        Long buildingId,
        String firstName,
        String lastName,
        String rut,
        String email,
        String phone,
        String position,
        boolean active
) {
}
