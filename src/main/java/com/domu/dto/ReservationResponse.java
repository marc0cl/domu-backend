package com.domu.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservationResponse(
        Long id,
        Long amenityId,
        String amenityName,
        Long userId,
        String userName,
        String userEmail,
        Long timeSlotId,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate reservationDate,
        String status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt) {
}
