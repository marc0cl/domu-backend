package com.domu.dto;

import jakarta.validation.constraints.NotBlank;

public record CommunityRequest(
        @NotBlank String name,
        @NotBlank String address,
        String city,
        String country,
        Integer maxUnits
) {
}
