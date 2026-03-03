package com.domu.dto;

import java.time.LocalDate;

public record ServiceOrderRequest(
        Long buildingId,
        Long providerId,
        String title,
        String description,
        LocalDate scheduledDate,
        String priority,
        String adminNotes
) {
}
