package com.domu.backend.domain.facility;

import com.domu.backend.domain.Identifiable;

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
) implements Identifiable<Reservation> {
    public Reservation {
        Objects.requireNonNull(commonAreaId, "commonAreaId");
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(reservationDate, "reservationDate");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");
    }

    @Override
    public Reservation withId(Long newId) {
        return new Reservation(newId, commonAreaId, unitId, reservationDate, startTime, endTime, comment, status);
    }
}
