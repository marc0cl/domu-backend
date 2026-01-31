package com.domu.dto;

import java.time.LocalTime;

public record TimeSlotResponse(
        Long id,
        Long amenityId,
        Integer dayOfWeek,
        String dayName,
        LocalTime startTime,
        LocalTime endTime,
        Boolean active) {

    public static String getDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miércoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sábado";
            case 7 -> "Domingo";
            default -> "Desconocido";
        };
    }
}
