package com.domu.dto;

public record BuildingSummaryResponse(
        Long id,
        String name,
        String address,
        String commune,
        String city
) {
}

