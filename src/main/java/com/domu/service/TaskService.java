package com.domu.service;

import com.domu.database.TaskRepository;
import com.domu.domain.core.User;
import com.domu.dto.TaskRequest;
import com.google.inject.Inject;

import java.util.List;

public class TaskService {

    private final TaskRepository repository;

    @Inject
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public List<TaskRepository.TaskResponse> listByBuilding(User user, Long buildingId) {
        // En un caso real validaríamos que el usuario pertenezca al edificio
        return repository.findByBuilding(buildingId);
    }

    public TaskRepository.TaskResponse create(User user, TaskRequest request) {
        // Validar que los staff asignados pertenezcan al mismo community que el building
        List<Long> assigneeIds = request.assigneeIds();
        if (assigneeIds == null && request.assigneeId() != null) {
            // Compatibilidad: convertir assigneeId único a lista
            assigneeIds = List.of(request.assigneeId());
        }
        
        if (assigneeIds != null && !assigneeIds.isEmpty()) {
            repository.validateStaffBelongsToBuilding(request.communityId(), assigneeIds);
        }
        
        return repository.insert(request, user != null ? user.id() : null);
    }

    public TaskRepository.TaskResponse update(User user, Long id, TaskRequest request) {
        // Obtener el building_id de la tarea existente para validar
        TaskRepository.TaskResponse existingTask = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + id));
        
        // Validar que los staff asignados pertenezcan al mismo community que el building
        List<Long> assigneeIds = request.assigneeIds();
        if (assigneeIds == null && request.assigneeId() != null) {
            // Compatibilidad: convertir assigneeId único a lista
            assigneeIds = List.of(request.assigneeId());
        }
        
        if (assigneeIds != null && !assigneeIds.isEmpty()) {
            repository.validateStaffBelongsToBuilding(existingTask.buildingId(), assigneeIds);
        }
        
        return repository.update(id, request);
    }

    public void delete(User user, Long id) {
        repository.delete(id);
    }
}
