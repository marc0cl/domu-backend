package com.domu.service;

import com.domu.database.VisitRepository;
import com.domu.domain.core.User;
import com.domu.dto.CreateVisitRequest;
import com.domu.dto.VisitListResponse;
import com.domu.dto.VisitResponse;
import com.google.inject.Inject;

import io.javalin.http.UnauthorizedResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VisitService {

    private static final Integer DEFAULT_VALID_MINUTES = 120;
    private static final Integer MAX_VALID_MINUTES = 24 * 60;

    private final VisitRepository visitRepository;

    @Inject
    public VisitService(VisitRepository visitRepository) {
        this.visitRepository = visitRepository;
    }

    public VisitResponse createVisit(User user, CreateVisitRequest request) {
        if (request == null) {
            throw new ValidationException("El cuerpo de la solicitud es obligatorio");
        }
        if (request.getVisitorName() == null || request.getVisitorName().isBlank()) {
            throw new ValidationException("visitorName es obligatorio");
        }
        Long unitId = resolveUnitForUser(user, request);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validFrom = request.getValidFrom() != null ? request.getValidFrom() : now;
        LocalDateTime validUntil = resolveValidUntil(request, validFrom);
        if (validUntil.isBefore(validFrom)) {
            throw new ValidationException("validUntil debe ser posterior a validFrom");
        }

        String normalizedName = request.getVisitorName().trim();
        String normalizedDocument = normalizeDocument(request.getVisitorDocument());
        String visitorType = normalizeVisitorType(request.getVisitorType());

        VisitRepository.VisitRow visit = visitRepository.insertVisit(new VisitRepository.VisitRow(
                null,
                normalizedName,
                normalizedDocument,
                visitorType,
                request.getCompany(),
                now
        ));

        VisitRepository.VisitAuthorizationRow authorization = visitRepository.insertAuthorization(new VisitRepository.VisitAuthorizationRow(
                null,
                visit.id(),
                user.id(),
                unitId,
                validFrom,
                validUntil,
                "SCHEDULED",
                null,
                now
        ));

        VisitRepository.VisitSummaryRow summary = new VisitRepository.VisitSummaryRow(
                authorization.id(),
                visit.id(),
                user.id(),
                unitId,
                visit.visitorName(),
                visit.visitorDocument(),
                visitorType,
                validFrom,
                validUntil,
                authorization.status(),
                authorization.createdAt(),
                null
        );

        return toResponse(summary, deriveStatus(summary, now));
    }

    public VisitListResponse getVisitsForUser(User user) {
        List<VisitRepository.VisitSummaryRow> rows;
        if (isResident(user)) {
            ensureResidentWithUnit(user);
            rows = visitRepository.findAuthorizationsForResident(user.id());
        } else if (isConcierge(user) || isAdmin(user)) {
            // Para conserjes/admins, mostramos las que ellos registraron
            rows = visitRepository.findAuthorizationsForResident(user.id());
        } else {
            throw new UnauthorizedResponse("No tienes permiso para ver visitas");
        }
        LocalDateTime now = LocalDateTime.now();
        List<VisitResponse> upcoming = new ArrayList<>();
        List<VisitResponse> past = new ArrayList<>();

        for (VisitRepository.VisitSummaryRow row : rows) {
            String status = deriveStatus(row, now);
            VisitResponse response = toResponse(row, status);
            if ("SCHEDULED".equalsIgnoreCase(status)) {
                upcoming.add(response);
            } else {
                past.add(response);
            }
        }
        return new VisitListResponse(upcoming, past);
    }

    public List<VisitResponse> getVisitHistory(User user, String search) {
        List<VisitRepository.VisitSummaryRow> rows;
        if (isResident(user)) {
            ensureResidentWithUnit(user);
            rows = visitRepository.searchAuthorizationsForResident(user.id(), search);
        } else if (isConcierge(user) || isAdmin(user)) {
            rows = visitRepository.searchAuthorizationsForResident(user.id(), search);
        } else {
            throw new UnauthorizedResponse("No tienes permiso para ver visitas previas");
        }
        LocalDateTime now = LocalDateTime.now();
        List<VisitResponse> history = new ArrayList<>();
        for (VisitRepository.VisitSummaryRow row : rows) {
            String status = deriveStatus(row, now);
            if (!"SCHEDULED".equalsIgnoreCase(status)) {
                history.add(toResponse(row, status));
            }
        }
        return history;
    }
    public VisitResponse registerCheckIn(Long authorizationId, User user) {
        VisitRepository.VisitSummaryRow existing;
        if (isResident(user)) {
            ensureResidentWithUnit(user);
            existing = visitRepository.findAuthorizationForResident(authorizationId, user.id())
                    .orElseThrow(() -> new UnauthorizedResponse("No puedes registrar visitas de otro residente"));
        } else if (isConcierge(user) || isAdmin(user)) {
            existing = visitRepository.findAuthorization(authorizationId)
                    .orElseThrow(() -> new ValidationException("Visita no encontrada"));
        } else {
            throw new UnauthorizedResponse("No tienes permiso para registrar ingresos");
        }
        LocalDateTime now = LocalDateTime.now();
        if (existing.validUntil().isBefore(now)) {
            visitRepository.updateAuthorizationStatus(existing.authorizationId(), "EXPIRED");
            throw new ValidationException("La visita ya expiró");
        }
        if ("CHECKED_IN".equalsIgnoreCase(existing.status())) {
            return toResponse(existing, "CHECKED_IN");
        }

        visitRepository.insertAccessLog(new VisitRepository.AccessLogRow(
                null,
                existing.visitId(),
                existing.authorizationId(),
                now,
                "MAIN_DOOR",
                user.id(),
                "CHECK_IN",
                now
        ));
        visitRepository.updateAuthorizationStatus(existing.authorizationId(), "CHECKED_IN");

        VisitRepository.VisitSummaryRow updated = new VisitRepository.VisitSummaryRow(
                existing.authorizationId(),
                existing.visitId(),
                existing.residentUserId(),
                existing.unitId(),
                existing.visitorName(),
                existing.visitorDocument(),
                existing.visitorType(),
                existing.validFrom(),
                existing.validUntil(),
                "CHECKED_IN",
                existing.createdAt(),
                now
        );

        return toResponse(updated, "CHECKED_IN");
    }

    private void ensureResidentWithUnit(User user) {
        if (user == null) {
            throw new UnauthorizedResponse("Debes iniciar sesión para gestionar visitas");
        }
        if (user.unitId() == null) {
            throw new ValidationException("El usuario no tiene una unidad asociada");
        }
        if (!Boolean.TRUE.equals(user.resident())) {
            throw new UnauthorizedResponse("Solo los residentes pueden registrar visitas");
        }
    }

    private Long resolveUnitForUser(User user, CreateVisitRequest request) {
        if (user == null) {
            throw new UnauthorizedResponse("Debes iniciar sesión para gestionar visitas");
        }
        if (isResident(user)) {
            ensureResidentWithUnit(user);
            return user.unitId();
        }
        if (isConcierge(user) || isAdmin(user)) {
            if (request.getUnitId() == null || request.getUnitId() <= 0) {
                throw new ValidationException("unitId es obligatorio para registrar visitas de terceros");
            }
            return request.getUnitId();
        }
        throw new UnauthorizedResponse("No tienes permiso para registrar visitas");
    }

    private boolean isResident(User user) {
        return Boolean.TRUE.equals(user.resident());
    }

    private boolean isConcierge(User user) {
        return user != null && user.roleId() != null && user.roleId() == 3;
    }

    private boolean isAdmin(User user) {
        return user != null && user.roleId() != null && user.roleId() == 1;
    }

    private LocalDateTime resolveValidUntil(CreateVisitRequest request, LocalDateTime validFrom) {
        if (request.getValidUntil() != null) {
            return request.getValidUntil();
        }
        Integer requestedMinutes = request.getValidForMinutes();
        Integer minutes = requestedMinutes != null && requestedMinutes > 0
                ? Math.min(requestedMinutes, MAX_VALID_MINUTES)
                : DEFAULT_VALID_MINUTES;
        return validFrom.plusMinutes(minutes);
    }

    private String normalizeDocument(String document) {
        if (document == null) {
            return null;
        }
        return document.replace(".", "").replace(" ", "").toUpperCase();
    }

    private String normalizeVisitorType(String visitorType) {
        if (visitorType == null || visitorType.isBlank()) {
            return "VISIT";
        }
        return visitorType.trim().toUpperCase();
    }

    private String deriveStatus(VisitRepository.VisitSummaryRow row, LocalDateTime now) {
        if ("CHECKED_IN".equalsIgnoreCase(row.status())) {
            return "CHECKED_IN";
        }
        if (row.validUntil().isBefore(now)) {
            return "EXPIRED";
        }
        return "SCHEDULED";
    }

    private VisitResponse toResponse(VisitRepository.VisitSummaryRow row, String status) {
        return new VisitResponse(
                row.authorizationId(),
                row.visitId(),
                row.visitorName(),
                row.visitorDocument(),
                row.visitorType(),
                row.unitId(),
                row.validFrom(),
                row.validUntil(),
                status,
                row.createdAt(),
                row.checkInAt()
        );
    }
}

