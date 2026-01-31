package com.domu.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AvailabilityResponse(
        Long amenityId,
        String amenityName,
        LocalDate date,
        String dayName,
        List<SlotAvailability> slots) {

    public record SlotAvailability(
            Long slotId,
            LocalTime startTime,
            LocalTime endTime,
            Boolean available,
            String reservedBy) {
    }
}
