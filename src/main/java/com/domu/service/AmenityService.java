package com.domu.service;

import com.domu.database.AmenityRepository;
import com.domu.database.AmenityRepository.AmenityRow;
import com.domu.database.AmenityRepository.ReservationRow;
import com.domu.database.AmenityRepository.TimeSlotRow;
import com.domu.database.BuildingRepository;
import com.domu.database.UserBuildingRepository;
import com.domu.domain.core.User;
import com.domu.dto.AmenityListResponse;
import com.domu.dto.AmenityRequest;
import com.domu.dto.AmenityResponse;
import com.domu.dto.AvailabilityResponse;
import com.domu.dto.AvailabilityResponse.SlotAvailability;
import com.domu.dto.ReservationListResponse;
import com.domu.dto.ReservationRequest;
import com.domu.dto.ReservationResponse;
import com.domu.dto.TimeSlotRequest;
import com.domu.dto.TimeSlotResponse;

import com.google.inject.Inject;

import io.javalin.http.UnauthorizedResponse;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final BuildingRepository buildingRepository;
    private final UserBuildingRepository userBuildingRepository;

    @Inject
    public AmenityService(AmenityRepository amenityRepository, BuildingRepository buildingRepository,
            UserBuildingRepository userBuildingRepository) {
        this.amenityRepository = amenityRepository;
        this.buildingRepository = buildingRepository;
        this.userBuildingRepository = userBuildingRepository;
    }

    // ==================== AMENITIES ====================

    public AmenityResponse createAmenity(User user, AmenityRequest request) {
        ensureAdminOrConcierge(user);
        validateAmenityRequest(request);
        Long buildingId = resolveBuildingId(user, request.getBuildingId());

        AmenityRow saved = amenityRepository.insertAmenity(new AmenityRow(
                null,
                buildingId,
                request.getName().trim(),
                request.getDescription(),
                request.getMaxCapacity(),
                request.getCostPerSlot() != null ? request.getCostPerSlot() : BigDecimal.ZERO,
                request.getRules(),
                request.getImageUrl(),
                request.getStatus() != null ? request.getStatus() : "ACTIVE",
                null,
                null));

        return toAmenityResponse(saved, List.of());
    }

    public AmenityResponse updateAmenity(User user, Long amenityId, AmenityRequest request) {
        ensureAdminOrConcierge(user);
        AmenityRow existing = amenityRepository.findAmenityById(amenityId)
                .orElseThrow(() -> new ValidationException("Área común no encontrada"));
        ensureSameBuilding(user, existing.buildingId());
        validateAmenityRequest(request);

        AmenityRow updated = amenityRepository.updateAmenity(new AmenityRow(
                amenityId,
                existing.buildingId(),
                request.getName().trim(),
                request.getDescription(),
                request.getMaxCapacity(),
                request.getCostPerSlot() != null ? request.getCostPerSlot() : BigDecimal.ZERO,
                request.getRules(),
                request.getImageUrl(),
                request.getStatus() != null ? request.getStatus() : existing.status(),
                existing.createdAt(),
                null));

        List<TimeSlotRow> slots = amenityRepository.findTimeSlotsByAmenity(amenityId);
        return toAmenityResponse(updated, slots);
    }

    public void deleteAmenity(User user, Long amenityId) {
        ensureAdminOrConcierge(user);
        AmenityRow existing = amenityRepository.findAmenityById(amenityId)
                .orElseThrow(() -> new ValidationException("Área común no encontrada"));
        ensureSameBuilding(user, existing.buildingId());
        amenityRepository.deleteAmenity(amenityId);
    }

    public AmenityResponse getAmenity(User user, Long amenityId) {
        ensureAuthenticated(user);
        AmenityRow amenity = amenityRepository.findAmenityById(amenityId)
                .orElseThrow(() -> new ValidationException("Área común no encontrada"));
        ensureSameBuilding(user, amenity.buildingId());
        List<TimeSlotRow> slots = amenityRepository.findTimeSlotsByAmenity(amenityId);
        return toAmenityResponse(amenity, slots);
    }

    public AmenityListResponse listAmenities(User user) {
        ensureAuthenticated(user);
        Long buildingId = resolveBuildingId(user, null);
        List<AmenityRow> amenities = amenityRepository.findAmenitiesByBuilding(buildingId);
        List<AmenityResponse> responses = new ArrayList<>();
        for (AmenityRow amenity : amenities) {
            List<TimeSlotRow> slots = amenityRepository.findTimeSlotsByAmenity(amenity.id());
            responses.add(toAmenityResponse(amenity, slots));
        }
        return new AmenityListResponse(responses);
    }

    public AmenityListResponse listAllAmenities(User user) {
        ensureAdminOrConcierge(user);
        Long buildingId = resolveBuildingId(user, null);
        List<AmenityRow> amenities = amenityRepository.findAllAmenitiesByBuilding(buildingId);
        List<AmenityResponse> responses = new ArrayList<>();
        for (AmenityRow amenity : amenities) {
            List<TimeSlotRow> slots = amenityRepository.findTimeSlotsByAmenity(amenity.id());
            responses.add(toAmenityResponse(amenity, slots));
        }
        return new AmenityListResponse(responses);
    }

    // ==================== TIME SLOTS ====================

    public AmenityResponse configureTimeSlots(User user, Long amenityId, TimeSlotRequest request) {
        ensureAdminOrConcierge(user);
        AmenityRow amenity = amenityRepository.findAmenityById(amenityId)
                .orElseThrow(() -> new ValidationException("Área común no encontrada"));
        ensureSameBuilding(user, amenity.buildingId());

        if (request == null || request.getSlots() == null || request.getSlots().isEmpty()) {
            throw new ValidationException("Debe proporcionar al menos un bloque horario");
        }

        // Validar cada slot
        for (TimeSlotRequest.TimeSlotItem item : request.getSlots()) {
            if (item.getDayOfWeek() == null || item.getDayOfWeek() < 1 || item.getDayOfWeek() > 7) {
                throw new ValidationException("dayOfWeek debe estar entre 1 (Lunes) y 7 (Domingo)");
            }
            if (item.getStartTime() == null || item.getEndTime() == null) {
                throw new ValidationException("startTime y endTime son obligatorios");
            }
            try {
                LocalTime start = LocalTime.parse(item.getStartTime());
                LocalTime end = LocalTime.parse(item.getEndTime());
                if (!start.isBefore(end)) {
                    throw new ValidationException("startTime debe ser anterior a endTime");
                }
            } catch (DateTimeParseException e) {
                throw new ValidationException("Formato de hora inválido. Use HH:mm (ej: 09:00)");
            }
        }

        // Eliminar slots existentes y crear nuevos
        amenityRepository.deleteTimeSlotsByAmenity(amenityId);

        List<TimeSlotRow> savedSlots = new ArrayList<>();
        for (TimeSlotRequest.TimeSlotItem item : request.getSlots()) {
            LocalTime start = LocalTime.parse(item.getStartTime());
            LocalTime end = LocalTime.parse(item.getEndTime());
            boolean active = item.getActive() != null ? item.getActive() : true;

            TimeSlotRow saved = amenityRepository.insertTimeSlot(new TimeSlotRow(
                    null,
                    amenityId,
                    item.getDayOfWeek(),
                    start,
                    end,
                    active,
                    null));
            savedSlots.add(saved);
        }

        return toAmenityResponse(amenity, savedSlots);
    }

    // ==================== AVAILABILITY ====================

    public AvailabilityResponse getAvailability(User user, Long amenityId, String dateStr) {
        ensureAuthenticated(user);
        AmenityRow amenity = amenityRepository.findAmenityById(amenityId)
                .orElseThrow(() -> new ValidationException("Área común no encontrada"));
        ensureSameBuilding(user, amenity.buildingId());

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new ValidationException("Formato de fecha inválido. Use YYYY-MM-DD");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new ValidationException("No se puede consultar disponibilidad para fechas pasadas");
        }

        // Obtener el día de la semana (1=Lunes, 7=Domingo)
        int dayOfWeek = date.getDayOfWeek().getValue();
        String dayName = TimeSlotResponse.getDayName(dayOfWeek);

        // Obtener slots activos para ese día
        List<TimeSlotRow> slots = amenityRepository.findActiveTimeSlotsByAmenityAndDay(amenityId, dayOfWeek);

        // Obtener reservas existentes para esa fecha
        List<ReservationRow> reservations = amenityRepository.findReservationsByAmenityAndDate(amenityId, date);

        // Construir respuesta de disponibilidad
        List<SlotAvailability> slotAvailabilities = new ArrayList<>();
        for (TimeSlotRow slot : slots) {
            boolean isReserved = reservations.stream()
                    .anyMatch(r -> Objects.equals(r.timeSlotId(), slot.id()));

            String reservedBy = null;
            if (isReserved) {
                ReservationRow reservation = reservations.stream()
                        .filter(r -> Objects.equals(r.timeSlotId(), slot.id()))
                        .findFirst()
                        .orElse(null);
                if (reservation != null) {
                    reservedBy = reservation.userFirstName() + " " + reservation.userLastName();
                }
            }

            slotAvailabilities.add(new SlotAvailability(
                    slot.id(),
                    slot.startTime(),
                    slot.endTime(),
                    !isReserved,
                    reservedBy));
        }

        return new AvailabilityResponse(amenityId, amenity.name(), date, dayName, slotAvailabilities);
    }

    // ==================== RESERVATIONS ====================

    public ReservationResponse createReservation(User user, Long amenityId, ReservationRequest request) {
        ensureAuthenticated(user);
        AmenityRow amenity = amenityRepository.findAmenityById(amenityId)
                .orElseThrow(() -> new ValidationException("Área común no encontrada"));
        ensureSameBuilding(user, amenity.buildingId());

        if (!"ACTIVE".equals(amenity.status())) {
            throw new ValidationException("El área común no está disponible para reservas");
        }

        if (request.getTimeSlotId() == null) {
            throw new ValidationException("timeSlotId es obligatorio");
        }
        if (request.getReservationDate() == null || request.getReservationDate().isBlank()) {
            throw new ValidationException("reservationDate es obligatorio");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(request.getReservationDate());
        } catch (Exception e) {
            throw new ValidationException("Formato de fecha inválido. Use YYYY-MM-DD");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new ValidationException("No se puede reservar para fechas pasadas");
        }

        // Verificar que el slot existe y pertenece al amenity
        TimeSlotRow slot = amenityRepository.findTimeSlotById(request.getTimeSlotId())
                .orElseThrow(() -> new ValidationException("Bloque horario no encontrado"));

        if (!Objects.equals(slot.amenityId(), amenityId)) {
            throw new ValidationException("El bloque horario no pertenece a esta área común");
        }

        if (!slot.active()) {
            throw new ValidationException("El bloque horario no está activo");
        }

        // Verificar que el día de la semana coincide
        int requestedDay = date.getDayOfWeek().getValue();
        if (!Objects.equals(slot.dayOfWeek(), requestedDay)) {
            throw new ValidationException("El bloque horario no está disponible para ese día de la semana");
        }

        // Verificar disponibilidad
        if (amenityRepository.isSlotReserved(request.getTimeSlotId(), date)) {
            throw new ValidationException("El bloque horario ya está reservado para esa fecha");
        }

        // Crear la reserva
        ReservationRow saved = amenityRepository.insertReservation(new ReservationRow(
                null,
                amenityId,
                user.id(),
                request.getTimeSlotId(),
                date,
                "CONFIRMED",
                request.getNotes(),
                null,
                null,
                null, null, null, null, null, null));

        return toReservationResponse(saved);
    }

    public ReservationResponse cancelReservation(User user, Long reservationId) {
        ensureAuthenticated(user);
        ReservationRow reservation = amenityRepository.findReservationById(reservationId)
                .orElseThrow(() -> new ValidationException("Reserva no encontrada"));

        // Solo el usuario que hizo la reserva o un admin puede cancelar
        boolean isOwner = Objects.equals(reservation.userId(), user.id());
        boolean isAdmin = isAdminOrConcierge(user);

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedResponse("No tienes permiso para cancelar esta reserva");
        }

        if ("CANCELLED".equals(reservation.status())) {
            throw new ValidationException("La reserva ya está cancelada");
        }

        if (reservation.reservationDate().isBefore(LocalDate.now())) {
            throw new ValidationException("No se puede cancelar una reserva pasada");
        }

        ReservationRow cancelled = amenityRepository.cancelReservation(reservationId);
        return toReservationResponse(cancelled);
    }

    public ReservationListResponse getMyReservations(User user) {
        ensureAuthenticated(user);
        List<ReservationRow> reservations = amenityRepository.findReservationsByUser(user.id());
        List<ReservationResponse> responses = reservations.stream()
                .map(this::toReservationResponse)
                .toList();
        return new ReservationListResponse(responses);
    }

    public ReservationListResponse getReservationsByAmenity(User user, Long amenityId) {
        ensureAdminOrConcierge(user);
        AmenityRow amenity = amenityRepository.findAmenityById(amenityId)
                .orElseThrow(() -> new ValidationException("Área común no encontrada"));
        ensureSameBuilding(user, amenity.buildingId());

        List<ReservationRow> reservations = amenityRepository.findReservationsByAmenity(amenityId);
        List<ReservationResponse> responses = reservations.stream()
                .map(this::toReservationResponse)
                .toList();
        return new ReservationListResponse(responses);
    }

    // ==================== HELPERS ====================

    private void validateAmenityRequest(AmenityRequest request) {
        if (request == null) {
            throw new ValidationException("El cuerpo es obligatorio");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException("El nombre es obligatorio");
        }
    }

    private void ensureAuthenticated(User user) {
        if (user == null) {
            throw new UnauthorizedResponse("Debes iniciar sesión");
        }
    }

    private void ensureAdminOrConcierge(User user) {
        ensureAuthenticated(user);
        if (!isAdminOrConcierge(user)) {
            throw new UnauthorizedResponse("Solo administradores o conserjes pueden realizar esta acción");
        }
    }

    private boolean isAdminOrConcierge(User user) {
        return user.roleId() != null && (Objects.equals(user.roleId(), 1L) || Objects.equals(user.roleId(), 3L));
    }

    private Long resolveBuildingId(User user, Long requestBuildingId) {
        if (requestBuildingId != null) {
            return requestBuildingId;
        }
        if (user != null && user.unitId() != null) {
            Long buildingId = buildingRepository.findBuildingIdByUnitId(user.unitId());
            if (buildingId != null) {
                return buildingId;
            }
        }
        if (user != null) {
            var buildings = userBuildingRepository.findBuildingsForUser(user.id());
            if (buildings != null && !buildings.isEmpty()) {
                return buildings.get(0).id();
            }
        }
        throw new ValidationException("No se pudo determinar el edificio asociado");
    }

    private void ensureSameBuilding(User user, Long buildingId) {
        Long userBuilding = resolveBuildingId(user, null);
        if (!Objects.equals(userBuilding, buildingId)) {
            throw new UnauthorizedResponse("No tienes acceso a esta área común");
        }
    }

    private AmenityResponse toAmenityResponse(AmenityRow amenity, List<TimeSlotRow> slots) {
        List<TimeSlotResponse> slotResponses = slots.stream()
                .map(slot -> new TimeSlotResponse(
                        slot.id(),
                        slot.amenityId(),
                        slot.dayOfWeek(),
                        TimeSlotResponse.getDayName(slot.dayOfWeek()),
                        slot.startTime(),
                        slot.endTime(),
                        slot.active()))
                .toList();

        return new AmenityResponse(
                amenity.id(),
                amenity.buildingId(),
                amenity.name(),
                amenity.description(),
                amenity.maxCapacity(),
                amenity.costPerSlot(),
                amenity.rules(),
                amenity.imageUrl(),
                amenity.status(),
                amenity.createdAt(),
                amenity.updatedAt(),
                slotResponses);
    }

    private ReservationResponse toReservationResponse(ReservationRow reservation) {
        String userName = null;
        if (reservation.userFirstName() != null || reservation.userLastName() != null) {
            userName = ((reservation.userFirstName() != null ? reservation.userFirstName() : "") + " "
                    + (reservation.userLastName() != null ? reservation.userLastName() : "")).trim();
        }
        return new ReservationResponse(
                reservation.id(),
                reservation.amenityId(),
                reservation.amenityName(),
                reservation.userId(),
                userName,
                reservation.userEmail(),
                reservation.timeSlotId(),
                reservation.startTime(),
                reservation.endTime(),
                reservation.reservationDate(),
                reservation.status(),
                reservation.notes(),
                reservation.createdAt(),
                reservation.cancelledAt());
    }
}
