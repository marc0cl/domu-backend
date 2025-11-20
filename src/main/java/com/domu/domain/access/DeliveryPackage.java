package com.domu.domain.access;

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
) {
    public DeliveryPackage {
        Objects.requireNonNull(visitId, "visitId");
        Objects.requireNonNull(receivedAt, "receivedAt");
        Objects.requireNonNull(description, "description");
    }
}
