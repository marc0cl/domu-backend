package com.domu.service;

import com.domu.database.BuildingRepository;
import com.domu.database.TaskRepository;
import com.domu.database.UserBuildingRepository;
import com.domu.domain.core.User;
import com.domu.dto.TaskRequest;
import com.google.inject.Inject;

import io.javalin.http.UnauthorizedResponse;

import java.util.List;
import java.util.Objects;

public class TaskService {

    private final TaskRepository repository;
    private final UserBuildingRepository userBuildingRepository;
    private final BuildingRepository buildingRepository;

    @Inject
    public TaskService(TaskRepository repository,
            UserBuildingRepository userBuildingRepository,
            BuildingRepository buildingRepository) {
        this.repository = repository;
        this.userBuildingRepository = userBuildingRepository;
        this.buildingRepository = buildingRepository;
    }

    public List<TaskRepository.TaskResponse> listByBuilding(User user, Long buildingId) {
        ensureUserHasAccessToBuilding(user, buildingId);
        return repository.findByBuilding(buildingId);
    }

    public TaskRepository.TaskResponse create(User user, Long selectedBuildingId, TaskRequest request) {
        ensureUserHasAccessToBuilding(user, selectedBuildingId);
        TaskRequest normalized = normalizeRequestForBuilding(request, selectedBuildingId);

        // Validar que los staff asignados pertenezcan al mismo community que el building
        List<Long> assigneeIds = normalized.assigneeIds();
        if (assigneeIds == null && normalized.assigneeId() != null) {
            // Compatibilidad: convertir assigneeId único a lista
            assigneeIds = List.of(normalized.assigneeId());
        }
        
        if (assigneeIds != null && !assigneeIds.isEmpty()) {
            repository.validateStaffBelongsToBuilding(selectedBuildingId, assigneeIds);
        }
        
        return repository.insert(normalized, user != null ? user.id() : null);
    }

    public TaskRepository.TaskResponse update(User user, Long selectedBuildingId, Long id, TaskRequest request) {
        ensureUserHasAccessToBuilding(user, selectedBuildingId);

        // Obtener el building_id de la tarea existente para validar
        TaskRepository.TaskResponse existingTask = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
        if (!Objects.equals(existingTask.buildingId(), selectedBuildingId)) {
            throw new UnauthorizedResponse("No tienes acceso a esta tarea");
        }
        TaskRequest normalized = normalizeRequestForBuilding(request, selectedBuildingId);
        
        // Validar que los staff asignados pertenezcan al mismo community que el building
        List<Long> assigneeIds = normalized.assigneeIds();
        if (assigneeIds == null && normalized.assigneeId() != null) {
            // Compatibilidad: convertir assigneeId único a lista
            assigneeIds = List.of(normalized.assigneeId());
        }
        
        if (assigneeIds != null && !assigneeIds.isEmpty()) {
            repository.validateStaffBelongsToBuilding(selectedBuildingId, assigneeIds);
        }
        
        return repository.update(id, normalized);
    }

    public void delete(User user, Long selectedBuildingId, Long id) {
        ensureUserHasAccessToBuilding(user, selectedBuildingId);
        TaskRepository.TaskResponse existingTask = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
        if (!Objects.equals(existingTask.buildingId(), selectedBuildingId)) {
            throw new UnauthorizedResponse("No tienes acceso a esta tarea");
        }
        repository.delete(id);
    }

    private TaskRequest normalizeRequestForBuilding(TaskRequest request, Long buildingId) {
        if (request == null) {
            throw new ValidationException("TaskRequest es obligatorio");
        }
        return new TaskRequest(
                buildingId,
                request.title(),
                request.description(),
                request.assigneeId(),
                request.assigneeIds(),
                request.status(),
                request.priority(),
                request.dueDate(),
                request.completedAt());
    }

    private void ensureUserHasAccessToBuilding(User user, Long buildingId) {
        if (user == null) {
            throw new UnauthorizedResponse("Debes iniciar sesión");
        }
        if (buildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        if (userBuildingRepository.userHasAccessToBuilding(user.id(), buildingId)) {
            return;
        }
        if (user.unitId() != null) {
            Long userBuilding = buildingRepository.findBuildingIdByUnitId(user.unitId());
            if (Objects.equals(userBuilding, buildingId)) {
                return;
            }
        }
        throw new UnauthorizedResponse("No tienes acceso a este edificio");
    }
}
