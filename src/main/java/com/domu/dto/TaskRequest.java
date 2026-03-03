package com.domu.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TaskRequest(
        Long communityId,
        String title,
        String description,
        Long assigneeId, // Mantenido para compatibilidad hacia atrás
        List<Long> assigneeIds, // Lista de IDs del personal asignado a la tarea
        String status,
        String priority,
        LocalDateTime dueDate,
        LocalDateTime completedAt
) {
}
