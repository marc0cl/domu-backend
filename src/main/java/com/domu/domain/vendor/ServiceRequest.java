package com.domu.domain.vendor;

import java.time.LocalDate;
import java.util.Objects;

public record ServiceRequest(
        Long id,
        Long providerId,
        String description,
        LocalDate requestedAt,
        String priority,
        String status
) {
    public ServiceRequest {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(requestedAt, "requestedAt");
    }
}
