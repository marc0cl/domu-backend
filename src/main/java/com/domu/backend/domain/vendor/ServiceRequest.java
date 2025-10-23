package com.domu.backend.domain.vendor;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDate;
import java.util.Objects;

public record ServiceRequest(
        Long id,
        Long providerId,
        String description,
        LocalDate requestedAt,
        String priority,
        String status
) implements Identifiable<ServiceRequest> {
    public ServiceRequest {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(requestedAt, "requestedAt");
    }

    @Override
    public ServiceRequest withId(Long newId) {
        return new ServiceRequest(newId, providerId, description, requestedAt, priority, status);
    }
}
