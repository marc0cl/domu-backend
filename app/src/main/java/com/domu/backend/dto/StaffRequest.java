package com.domu.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StaffRequest(
        @NotNull Long communityId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String rut,
        @Email String email,
        String phone,
        @NotBlank String position,
        boolean active
) {
}
