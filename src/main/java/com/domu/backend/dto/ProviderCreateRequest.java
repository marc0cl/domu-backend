package com.domu.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProviderCreateRequest(
        @NotNull Long communityId,
        @NotBlank String name,
        @NotBlank String serviceType,
        @Email String email,
        String phone,
        Integer rating
) {
}
