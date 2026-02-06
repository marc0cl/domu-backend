package com.domu.service;

import com.domu.database.StaffRepository;
import com.domu.domain.core.User;
import com.domu.dto.StaffRequest;
import com.google.inject.Inject;

import java.util.List;

public class StaffService {

    private final StaffRepository repository;

    @Inject
    public StaffService(StaffRepository repository) {
        this.repository = repository;
    }

    public List<StaffRepository.StaffResponse> listByBuilding(User user, Long buildingId) {
        // Validar que el usuario tenga acceso al building
        return repository.findByBuilding(buildingId);
    }

    public List<StaffRepository.StaffResponse> listActiveByBuilding(User user, Long buildingId) {
        // Validar que el usuario tenga acceso al building
        return repository.findActiveByBuilding(buildingId);
    }

    public StaffRepository.StaffResponse create(User user, StaffRequest request) {
        // Validar que el RUT no esté duplicado
        repository.findByRut(request.rut()).ifPresent(existing -> {
            throw new RuntimeException("Ya existe un miembro del personal con este RUT: " + request.rut());
        });
        
        // Validar que el email no esté duplicado si se proporciona
        if (request.email() != null && !request.email().isBlank()) {
            repository.findByEmail(request.email()).ifPresent(existing -> {
                throw new RuntimeException("Ya existe un miembro del personal con este email: " + request.email());
            });
        }
        
        return repository.insert(request);
    }

    public StaffRepository.StaffResponse update(User user, Long id, StaffRequest request) {
        // Verificar que el staff existe
        StaffRepository.StaffResponse existing = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + id));
        
        // Validar que el RUT no esté duplicado (excepto para el mismo registro)
        repository.findByRut(request.rut()).ifPresent(staff -> {
            if (!staff.id().equals(id)) {
                throw new RuntimeException("Ya existe otro miembro del personal con este RUT: " + request.rut());
            }
        });
        
        // Validar que el email no esté duplicado si se proporciona
        if (request.email() != null && !request.email().isBlank()) {
            repository.findByEmail(request.email()).ifPresent(staff -> {
                if (!staff.id().equals(id)) {
                    throw new RuntimeException("Ya existe otro miembro del personal con este email: " + request.email());
                }
            });
        }
        
        return repository.update(id, request);
    }

    public void delete(User user, Long id) {
        // Verificar que el staff existe
        repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + id));
        
        repository.delete(id);
    }

    public StaffRepository.StaffResponse findById(User user, Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + id));
    }
}
