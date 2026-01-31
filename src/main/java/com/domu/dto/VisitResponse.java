package com.domu.dto;

import java.time.LocalDateTime;

public record VisitResponse(
        Long authorizationId,
        Long visitId,
        String visitorName,
        String visitorDocument,
        String visitorType,
        Long unitId,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        String status,
        LocalDateTime createdAt,
        LocalDateTime checkInAt
) {
}

