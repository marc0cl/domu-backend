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
        String boxFolderId,
        String boxFileId,
        String boxFileName,
        String status,
        LocalDateTime createdAt,
        Long reviewedByUserId,
        LocalDateTime reviewedAt,
        String reviewNotes,
        Long buildingId,
        String approvalCode,
        LocalDateTime approvalCodeExpiresAt,
        LocalDateTime approvalCodeUsedAt,
        String approvalAction,
        String adminInviteCode,
        LocalDateTime adminInviteExpiresAt,
        LocalDateTime adminInviteUsedAt
) {
    public BuildingRequest {
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
                boxFolderId,
                boxFileId,
                boxFileName,
                status,
                createdAt,
                reviewedByUserId,
                reviewedAt,
                reviewNotes,
                buildingId,
                approvalCode,
                approvalCodeExpiresAt,
                approvalCodeUsedAt,
                approvalAction,
                adminInviteCode,
                adminInviteExpiresAt,
                adminInviteUsedAt
        );
    }
}

