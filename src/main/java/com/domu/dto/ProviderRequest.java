package com.domu.dto;

public record ProviderRequest(
        Long buildingId,
        Long userId,
        String businessName,
        String rut,
        String contactName,
        String email,
        String phone,
        String address,
        String serviceCategory,
        boolean active
) {
}
