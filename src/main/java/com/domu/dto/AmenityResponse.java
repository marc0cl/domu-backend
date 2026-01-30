package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AmenityResponse(
        Long id,
        Long buildingId,
        String name,
        String description,
        Integer maxCapacity,
        BigDecimal costPerSlot,
        String rules,
        String imageUrl,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TimeSlotResponse> timeSlots) {
}
