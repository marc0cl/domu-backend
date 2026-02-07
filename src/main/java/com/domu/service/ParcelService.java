package com.domu.service;

import com.domu.database.HousingUnitRepository;
import com.domu.database.ParcelRepository;
import com.domu.domain.core.HousingUnit;
import com.domu.domain.core.User;
import com.domu.dto.ParcelRequest;
import com.domu.dto.ParcelResponse;
import com.google.inject.Inject;

import io.javalin.http.UnauthorizedResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class ParcelService {

    private final ParcelRepository parcelRepository;
    private final HousingUnitRepository housingUnitRepository;

    @Inject
    public ParcelService(ParcelRepository parcelRepository, HousingUnitRepository housingUnitRepository) {
        this.parcelRepository = parcelRepository;
        this.housingUnitRepository = housingUnitRepository;
    }

    public ParcelResponse create(User user, Long buildingId, ParcelRequest request) {
        ensureAdminOrConcierge(user);
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        validateCreate(request);
        HousingUnit unit = housingUnitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new ValidationException("Unidad no encontrada"));
        if (!Objects.equals(unit.buildingId(), buildingId)) {
            throw new ValidationException("La unidad no pertenece al edificio seleccionado");
        }

        LocalDateTime receivedAt = request.getReceivedAt() != null ? request.getReceivedAt() : LocalDateTime.now();
        ParcelRepository.ParcelRow saved = parcelRepository.insert(new ParcelRepository.ParcelRow(
                null,
                buildingId,
                unit.id(),
                unit.number(),
                unit.tower(),
                unit.floor(),
                user.id(),
                null,
                request.getSender().trim(),
                request.getDescription().trim(),
                "PENDING",
                receivedAt,
                null,
                null,
                null));
        return toResponse(saved);
    }

    public List<ParcelResponse> listMyParcels(User user, String status) {
        ensureAuthenticated(user);
        ensureUnitAssigned(user);
        String normalizedStatus = normalizeStatus(status);
        HousingUnit unit = housingUnitRepository.findById(user.unitId())
                .orElseThrow(() -> new ValidationException("Unidad no encontrada para el usuario"));
        return parcelRepository.findByBuildingAndUnitNumber(unit.buildingId(), unit.number(), normalizedStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ParcelResponse> listForBuilding(User user, Long buildingId, String status, Long unitId) {
        ensureAdminOrConcierge(user);
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        if (unitId != null) {
            HousingUnit unit = housingUnitRepository.findById(unitId)
                    .orElseThrow(() -> new ValidationException("Unidad no encontrada"));
            if (!Objects.equals(unit.buildingId(), buildingId)) {
                throw new ValidationException("La unidad no pertenece al edificio seleccionado");
            }
        }
        String normalizedStatus = normalizeStatus(status);
        return parcelRepository.findByBuilding(buildingId, normalizedStatus, unitId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ParcelResponse updateStatus(User user, Long parcelId, Long buildingId, String status) {
        ensureAdminOrConcierge(user);
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        String normalizedStatus = normalizeStatus(status);
        if (!"COLLECTED".equals(normalizedStatus)) {
            throw new ValidationException("El estado permitido es COLLECTED");
        }
        ParcelRepository.ParcelRow existing = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new ValidationException("Encomienda no encontrada"));
        if (!Objects.equals(existing.buildingId(), buildingId)) {
            throw new ValidationException("La encomienda no pertenece al edificio seleccionado");
        }
        if ("COLLECTED".equalsIgnoreCase(existing.status())) {
            return toResponse(existing);
        }
        ParcelRepository.ParcelRow updated = parcelRepository.updateStatus(
                parcelId,
                "COLLECTED",
                user.id(),
                LocalDateTime.now(),
                buildingId);
        return toResponse(updated);
    }

    public ParcelResponse update(User user, Long parcelId, Long buildingId, ParcelRequest request) {
        ensureAdminOrConcierge(user);
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        validateCreate(request);
        ParcelRepository.ParcelRow existing = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new ValidationException("Encomienda no encontrada"));
        if (!Objects.equals(existing.buildingId(), buildingId)) {
            throw new ValidationException("La encomienda no pertenece al edificio seleccionado");
        }
        HousingUnit unit = housingUnitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new ValidationException("Unidad no encontrada"));
        if (!Objects.equals(unit.buildingId(), buildingId)) {
            throw new ValidationException("La unidad no pertenece al edificio seleccionado");
        }
        LocalDateTime receivedAt = request.getReceivedAt() != null ? request.getReceivedAt() : existing.receivedAt();
        ParcelRepository.ParcelRow updated = parcelRepository.update(new ParcelRepository.ParcelRow(
                existing.id(),
                buildingId,
                unit.id(),
                unit.number(),
                unit.tower(),
                unit.floor(),
                existing.receivedByUserId(),
                existing.retrievedByUserId(),
                request.getSender().trim(),
                request.getDescription().trim(),
                existing.status(),
                receivedAt,
                existing.retrievedAt(),
                existing.createdAt(),
                existing.updatedAt()));
        return toResponse(updated);
    }

    public void delete(User user, Long parcelId, Long buildingId) {
        ensureAdminOrConcierge(user);
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        ParcelRepository.ParcelRow existing = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new ValidationException("Encomienda no encontrada"));
        if (!Objects.equals(existing.buildingId(), buildingId)) {
            throw new ValidationException("La encomienda no pertenece al edificio seleccionado");
        }
        parcelRepository.delete(parcelId, buildingId);
    }

    private ParcelResponse toResponse(ParcelRepository.ParcelRow row) {
        return new ParcelResponse(
                row.id(),
                row.buildingId(),
                row.unitId(),
                row.unitNumber(),
                row.unitTower(),
                row.unitFloor(),
                row.receivedByUserId(),
                row.retrievedByUserId(),
                row.sender(),
                row.description(),
                row.status(),
                row.receivedAt(),
                row.retrievedAt(),
                row.createdAt(),
                row.updatedAt());
    }

    private void validateCreate(ParcelRequest request) {
        if (request == null) {
            throw new ValidationException("El cuerpo de la solicitud es obligatorio");
        }
        if (request.getUnitId() == null || request.getUnitId() <= 0) {
            throw new ValidationException("unitId es obligatorio");
        }
        if (request.getSender() == null || request.getSender().isBlank()) {
            throw new ValidationException("sender es obligatorio");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ValidationException("description es obligatorio");
        }
    }

    private void ensureAuthenticated(User user) {
        if (user == null) {
            throw new UnauthorizedResponse("Debes iniciar sesión");
        }
    }

    private void ensureUnitAssigned(User user) {
        if (user.unitId() == null) {
            throw new ValidationException("El usuario no tiene una unidad asociada");
        }
    }

    private boolean isAdminOrConcierge(User user) {
        if (user == null || user.roleId() == null) {
            return false;
        }
        return Objects.equals(user.roleId(), 1L) || Objects.equals(user.roleId(), 3L);
    }

    private void ensureAdminOrConcierge(User user) {
        if (!isAdminOrConcierge(user)) {
            throw new UnauthorizedResponse("Solo administradores o conserjes pueden gestionar encomiendas");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "PENDING", "COLLECTED" -> normalized;
            default -> null;
        };
    }
}
