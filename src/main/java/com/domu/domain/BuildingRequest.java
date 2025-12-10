package com.domu.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record BuildingRequest(
        Long id,
        Long requestedByUserId,
        String name,
        String towerLabel,
        String address,
        String commune,
        String city,
        String adminPhone,
        String adminEmail,
        String adminName,
        String adminDocument,
        Integer floors,
        Integer unitsCount,
        Double latitude,
        Double longitude,
        String proofText,
        String status,
        LocalDateTime createdAt,
        Long reviewedByUserId,
        LocalDateTime reviewedAt,
        String reviewNotes,
        Long buildingId
) {
    public BuildingRequest {
        Objects.requireNonNull(requestedByUserId, "requestedByUserId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(address, "address");
    }

    public BuildingRequest withId(Long newId) {
        return new BuildingRequest(
                newId,
                requestedByUserId,
                name,
                towerLabel,
                address,
                commune,
                city,
                adminPhone,
                adminEmail,
                adminName,
                adminDocument,
                floors,
                unitsCount,
                latitude,
                longitude,
                proofText,
                status,
                createdAt,
                reviewedByUserId,
                reviewedAt,
                reviewNotes,
                buildingId
        );
    }
}

