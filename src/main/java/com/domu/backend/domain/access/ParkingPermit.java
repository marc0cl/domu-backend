package com.domu.backend.domain.access;

import com.domu.backend.domain.Identifiable;

import java.time.LocalDateTime;

public record ParkingPermit(
        Long id,
        Long parkingSpaceId,
        Long residentUserId,
        Long visitId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String permitType,
        String status
) implements Identifiable<ParkingPermit> {
    @Override
    public ParkingPermit withId(Long newId) {
        return new ParkingPermit(newId, parkingSpaceId, residentUserId, visitId, startTime, endTime, permitType, status);
    }
}
