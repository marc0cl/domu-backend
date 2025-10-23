package com.domu.backend.domain.access;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;
import java.util.Objects;

public record DeliveryPackage(
        Long id,
        Long visitId,
        Long receivedByUserId,
        LocalDateTime receivedAt,
        String description,
        String photoUrl,
        String status
) implements Identifiable<DeliveryPackage> {
    public DeliveryPackage {
        Objects.requireNonNull(visitId, "visitId");
        Objects.requireNonNull(receivedAt, "receivedAt");
        Objects.requireNonNull(description, "description");
    }

    @Override
    public DeliveryPackage withId(Long newId) {
        return new DeliveryPackage(newId, visitId, receivedByUserId, receivedAt, description, photoUrl, status);
    }
}
