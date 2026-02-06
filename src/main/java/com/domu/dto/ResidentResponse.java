package com.domu.dto;

import java.time.LocalDateTime;

public record ResidentResponse(
        Long id,
        Long unitId,
        String unitNumber,
        String tower,
        String floor,
        Long roleId,
        String roleName,
        String firstName,
        String lastName,
        String email,
        String phone,
        String documentNumber,
        Boolean resident,
        LocalDateTime createdAt,
        String status) {
    public static ResidentResponse from(
            com.domu.database.UserRepository.ResidentWithUnit r) {
        String roleName = switch (r.roleId() != null ? r.roleId().intValue() : 0) {
            case 1 -> "Administrador";
            case 2 -> "Residente";
            case 3 -> "Conserje";
            case 4 -> "Personal";
            default -> "Usuario";
        };
        return new ResidentResponse(
                r.id(),
                r.unitId(),
                r.unitNumber(),
                r.tower(),
                r.floor(),
                r.roleId(),
                roleName,
                r.firstName(),
                r.lastName(),
                r.email(),
                r.phone(),
                r.documentNumber(),
                r.resident(),
                r.createdAt(),
                r.status());
    }
}
