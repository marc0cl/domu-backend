package com.domu.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResidentRequest(
        @NotNull Long communityId,
        Long unitId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email @NotBlank String email,
        String phone,
        @NotBlank String rut,
        boolean owner,
        boolean active
) {
}
