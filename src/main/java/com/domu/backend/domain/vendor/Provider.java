package com.domu.backend.domain.vendor;

import com.domu.backend.domain.Identifiable;

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
) implements Identifiable<Provider> {
    public Provider {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(contactName, "contactName");
    }

    @Override
    public Provider withId(Long newId) {
        return new Provider(newId, buildingId, name, documentNumber, businessLine, contactName, contactPhone, contactEmail, address, status);
    }
}
