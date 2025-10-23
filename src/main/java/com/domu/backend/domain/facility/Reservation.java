package com.domu.backend.domain.facility;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record Reservation(
        Long id,
        Long commonAreaId,
        Long unitId,
        LocalDate reservationDate,
        LocalTime startTime,
        LocalTime endTime,
        String comment,
        String status
) {
    public Reservation {
        Objects.requireNonNull(commonAreaId, "commonAreaId");
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(reservationDate, "reservationDate");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");
    }
}
