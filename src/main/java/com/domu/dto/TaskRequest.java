package com.domu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record TaskRequest(
        @NotNull Long communityId,
        @NotBlank String title,
        String description,
        Long assigneeId, // Mantenido para compatibilidad hacia atrás
        List<Long> assigneeIds, // Lista de IDs del personal asignado a la tarea
        String status,
        String priority,
        LocalDateTime dueDate,
        LocalDateTime completedAt
) {
}
