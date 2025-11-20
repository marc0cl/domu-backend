package com.domu.backend.domain.vendor;

import java.util.Objects;

public record Provider(
        Long id,
        Long buildingId,
        String name,
        String documentNumber,
        String businessLine,
        String contactName,
        String contactPhone,
        String contactEmail,
        String address,
        String status
) {
    public Provider {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(contactName, "contactName");
    }
}
