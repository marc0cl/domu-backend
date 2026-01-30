package com.domu.service;

import com.domu.database.HousingUnitRepository;
import com.domu.database.UserBuildingRepository;
import com.domu.database.UserRepository;
import com.domu.domain.core.HousingUnit;
import com.domu.domain.core.User;
import com.domu.dto.HousingUnitRequest;
import com.domu.dto.HousingUnitResponse;
import com.domu.dto.HousingUnitWithResidents;
import com.google.inject.Inject;

import java.util.List;

public class HousingUnitService {

    private static final Long ADMIN_ROLE_ID = 1L;

    private final HousingUnitRepository housingUnitRepository;
    private final UserBuildingRepository userBuildingRepository;
    private final UserRepository userRepository;

    @Inject
    public HousingUnitService(
            HousingUnitRepository housingUnitRepository,
            UserBuildingRepository userBuildingRepository,
            UserRepository userRepository) {
        this.housingUnitRepository = housingUnitRepository;
        this.userBuildingRepository = userBuildingRepository;
        this.userRepository = userRepository;
    }

    /**
     * Crear una nueva unidad habitacional
     */
    public HousingUnitResponse create(Long userId, Long buildingId, HousingUnitRequest request) {
        validateAdminAccess(userId, buildingId);
        validateRequest(request);

        // Validar que no exista una unidad con el mismo número en el mismo edificio
        if (housingUnitRepository.existsByNumberAndBuildingId(request.getNumber(), buildingId, null)) {
            throw new ValidationException(
                    "Ya existe una unidad con el número '" + request.getNumber() + "' en este edificio");
        }

        HousingUnit unit = new HousingUnit(
                null,
                buildingId,
                request.getNumber().trim(),
                request.getTower() != null ? request.getTower().trim() : null,
                request.getFloor() != null ? request.getFloor().trim() : null,
                request.getAliquotPercentage(),
                request.getSquareMeters(),
                "ACTIVE",
                userId,
                null,
                null);

        HousingUnit created = housingUnitRepository.insert(unit);
        return housingUnitRepository.findByIdWithDetails(created.id())
                .orElseThrow(() -> new ValidationException("Error al obtener la unidad creada"));
    }

    /**
     * Actualizar una unidad existente
     */
    public HousingUnitResponse update(Long userId, Long unitId, HousingUnitRequest request) {
        validateRequest(request);

        // Verificar que la unidad existe
        HousingUnit existing = housingUnitRepository.findById(unitId)
                .orElseThrow(() -> new ValidationException("Unidad no encontrada"));

        // Validar acceso del admin al edificio
        validateAdminAccess(userId, existing.buildingId());

        // Validar que no exista otra unidad con el mismo número (excluyendo la actual)
        if (housingUnitRepository.existsByNumberAndBuildingId(
                request.getNumber(), existing.buildingId(), unitId)) {
            throw new ValidationException(
                    "Ya existe otra unidad con el número '" + request.getNumber() + "' en este edificio");
        }

        HousingUnit updated = new HousingUnit(
                existing.id(),
                existing.buildingId(),
                request.getNumber().trim(),
                request.getTower() != null ? request.getTower().trim() : null,
                request.getFloor() != null ? request.getFloor().trim() : null,
                request.getAliquotPercentage(),
                request.getSquareMeters(),
                existing.status(),
                existing.createdByUserId(),
                existing.createdAt(),
                existing.updatedAt());

        housingUnitRepository.update(updated);
        return housingUnitRepository.findByIdWithDetails(unitId)
                .orElseThrow(() -> new ValidationException("Error al obtener la unidad actualizada"));
    }

    /**
     * Eliminar una unidad (soft delete)
     */
    public void delete(Long userId, Long unitId) {
        // Verificar que la unidad existe
        HousingUnit existing = housingUnitRepository.findById(unitId)
                .orElseThrow(() -> new ValidationException("Unidad no encontrada"));

        // Validar acceso del admin al edificio
        validateAdminAccess(userId, existing.buildingId());

        // Validar que no tenga residentes activos
        int residentCount = housingUnitRepository.countResidentsByUnitId(unitId);
        if (residentCount > 0) {
            throw new ValidationException(
                    "No se puede eliminar la unidad porque tiene " + residentCount +
                            " residente(s) asignado(s). Primero desvincule a todos los residentes.");
        }

        housingUnitRepository.softDelete(unitId);
    }

    /**
     * Listar unidades de un edificio
     */
    public List<HousingUnitWithResidents> listByBuilding(Long userId, Long buildingId) {
        validateAdminAccess(userId, buildingId);
        return housingUnitRepository.findByBuildingIdWithResidents(buildingId);
    }

    /**
     * Obtener detalle de una unidad
     */
    public HousingUnitResponse getById(Long userId, Long unitId) {
        HousingUnit unit = housingUnitRepository.findById(unitId)
                .orElseThrow(() -> new ValidationException("Unidad no encontrada"));

        validateAdminAccess(userId, unit.buildingId());

        return housingUnitRepository.findByIdWithDetails(unitId)
                .orElseThrow(() -> new ValidationException("Error al obtener los detalles de la unidad"));
    }

    /**
     * Vincular un residente a una unidad
     */
    public void linkResident(Long adminUserId, Long residentUserId, Long unitId) {
        // Validar parámetros
        if (unitId == null) {
            throw new ValidationException("El ID de la unidad no puede ser nulo");
        }
        if (residentUserId == null) {
            throw new ValidationException("El ID del residente no puede ser nulo");
        }

        // Verificar que la unidad existe
        HousingUnit unit = housingUnitRepository.findById(unitId)
                .orElseThrow(() -> new ValidationException(
                        "Unidad no encontrada con id: " + unitId +
                                ". Verifica que la unidad exista en la base de datos."));

        // Validar acceso del admin al edificio
        validateAdminAccess(adminUserId, unit.buildingId());

        // Verificar que el residente existe
        User resident = userRepository.findById(residentUserId)
                .orElseThrow(() -> new ValidationException("Residente no encontrado con id: " + residentUserId));

        // Validar que el residente tenga acceso al edificio (debe estar en
        // user_buildings)
        if (!userBuildingRepository.userHasAccessToBuilding(residentUserId, unit.buildingId())) {
            throw new ValidationException(
                    "El residente no tiene acceso a este edificio. " +
                            "Primero debe ser agregado al edificio antes de asignarlo a una unidad.");
        }

        // Actualizar el unit_id del usuario
        try {
            userRepository.updateUnitId(residentUserId, unitId);
        } catch (Exception e) {
            throw new ValidationException("Error al asignar residente a la unidad: " + e.getMessage());
        }
    }

    /**
     * Desvincular un residente de su unidad
     */
    public void unlinkResident(Long adminUserId, Long residentUserId) {
        // Verificar que el residente existe y obtener su unidad actual
        User resident = userRepository.findById(residentUserId)
                .orElseThrow(() -> new ValidationException("Residente no encontrado"));

        if (resident.unitId() == null) {
            throw new ValidationException("El residente no está vinculado a ninguna unidad");
        }

        // Verificar que la unidad existe
        HousingUnit unit = housingUnitRepository.findById(resident.unitId())
                .orElseThrow(() -> new ValidationException("Unidad no encontrada"));

        // Validar acceso del admin al edificio
        validateAdminAccess(adminUserId, unit.buildingId());

        // Desvincular (establecer unit_id a NULL)
        userRepository.updateUnitId(residentUserId, null);
    }

    /**
     * Validar que el usuario es admin y tiene acceso al edificio
     */
    private void validateAdminAccess(Long userId, Long buildingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("Usuario no encontrado"));

        // Verificar que es administrador
        if (user.roleId() == null || !ADMIN_ROLE_ID.equals(user.roleId())) {
            throw new ValidationException("Solo los administradores pueden gestionar unidades");
        }

        // Verificar que tiene acceso al edificio
        if (!userBuildingRepository.userHasAccessToBuilding(userId, buildingId)) {
            throw new ValidationException("No tienes acceso a este edificio");
        }
    }

    /**
     * Validar los datos del request
     */
    private void validateRequest(HousingUnitRequest request) {
        if (request.getNumber() == null || request.getNumber().trim().isEmpty()) {
            throw new ValidationException("El número de unidad es requerido");
        }

        if (request.getTower() == null || request.getTower().trim().isEmpty()) {
            throw new ValidationException("La torre es requerida");
        }

        if (request.getFloor() == null || request.getFloor().trim().isEmpty()) {
            throw new ValidationException("El piso es requerido");
        }

        // Validar que los valores numéricos sean positivos si están presentes
        if (request.getAliquotPercentage() != null &&
                request.getAliquotPercentage().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new ValidationException("La alícuota no puede ser negativa");
        }

        if (request.getSquareMeters() != null &&
                request.getSquareMeters().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Los metros cuadrados deben ser mayores a cero");
        }
    }
}
