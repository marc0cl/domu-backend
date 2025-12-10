package com.domu.service;

import com.domu.database.IncidentRepository;
import com.domu.domain.core.User;
import com.domu.dto.IncidentListResponse;
import com.domu.dto.IncidentRequest;
import com.domu.dto.IncidentResponse;
import com.google.inject.Inject;

import io.javalin.http.UnauthorizedResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IncidentService {

    private final IncidentRepository incidentRepository;

    @Inject
    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public IncidentResponse create(User user, IncidentRequest request) {
        ensureAuthenticated(user);
        validate(request);
        String status = normalizeStatus(request.getStatus());
        String priority = normalizePriority(request.getPriority());
        LocalDateTime createdAt = request.getCreatedAt() != null ? request.getCreatedAt() : LocalDateTime.now();

        IncidentRepository.IncidentRow saved = incidentRepository.insert(new IncidentRepository.IncidentRow(
                null,
                user.id(),
                user.unitId(),
                request.getTitle().trim(),
                request.getDescription().trim(),
                request.getCategory().trim(),
                priority,
                status,
                createdAt,
                createdAt
        ));

        return toResponse(saved);
    }

    public IncidentListResponse list(User user, LocalDate from, LocalDate to) {
        ensureAuthenticated(user);
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;
        List<IncidentRepository.IncidentRow> rows;
        if (isAdminOrConcierge(user)) {
            rows = incidentRepository.findAll(fromDateTime, toDateTime);
        } else {
            rows = incidentRepository.findByUser(user.id(), fromDateTime, toDateTime);
        }

        List<IncidentResponse> reported = new ArrayList<>();
        List<IncidentResponse> inProgress = new ArrayList<>();
        List<IncidentResponse> closed = new ArrayList<>();

        for (IncidentRepository.IncidentRow row : rows) {
            IncidentResponse response = toResponse(row);
            String status = row.status().toUpperCase();
            if ("IN_PROGRESS".equals(status)) {
                inProgress.add(response);
            } else if ("CLOSED".equals(status)) {
                closed.add(response);
            } else {
                reported.add(response);
            }
        }
        return new IncidentListResponse(reported, inProgress, closed);
    }

    private IncidentResponse toResponse(IncidentRepository.IncidentRow row) {
        return new IncidentResponse(
                row.id(),
                row.userId(),
                row.unitId(),
                row.title(),
                row.description(),
                row.category(),
                row.priority(),
                row.status(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private void validate(IncidentRequest request) {
        if (request == null) {
            throw new ValidationException("El cuerpo de la solicitud es obligatorio");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ValidationException("title es obligatorio");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ValidationException("description es obligatorio");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new ValidationException("category es obligatorio");
        }
    }

    private void ensureAuthenticated(User user) {
        if (user == null) {
            throw new UnauthorizedResponse("Debes iniciar sesión");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "REPORTED";
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "REPORTED", "IN_PROGRESS", "CLOSED" -> normalized;
            default -> "REPORTED";
        };
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "MEDIUM";
        }
        String normalized = priority.trim().toUpperCase();
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH" -> normalized;
            default -> "MEDIUM";
        };
    }

    private boolean isAdminOrConcierge(User user) {
        if (user == null || user.roleId() == null) {
            return false;
        }
        return Objects.equals(user.roleId(), 1L) || Objects.equals(user.roleId(), 3L);
    }
}

