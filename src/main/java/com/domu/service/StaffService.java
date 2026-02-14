package com.domu.service;

import com.domu.database.StaffRepository;
import com.domu.domain.core.User;
import com.domu.dto.StaffRequest;
import com.google.inject.Inject;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

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

    public Optional<StaffRepository.StaffResponse> findForUser(User user, Long buildingId) {
        if (user == null || buildingId == null) {
            return Optional.empty();
        }

        String normalizedUserDocument = normalizeDocument(user.documentNumber());
        if (normalizedUserDocument != null) {
            Optional<StaffRepository.StaffResponse> byRut = repository.findByRut(user.documentNumber().trim());
            if (byRut.isPresent() && Objects.equals(byRut.get().buildingId(), buildingId)) {
                return byRut;
            }
        }

        String normalizedUserEmail = normalizeEmail(user.email());
        if (user.email() != null && !user.email().isBlank()) {
            Optional<StaffRepository.StaffResponse> byEmail = repository.findByEmail(user.email().trim().toLowerCase(Locale.ROOT));
            if (byEmail.isPresent() && Objects.equals(byEmail.get().buildingId(), buildingId)) {
                return byEmail;
            }
        }

        String normalizedFirstName = normalizeText(user.firstName());
        String normalizedLastName = normalizeText(user.lastName());

        return repository.findByBuilding(buildingId).stream()
            .filter(staff -> {
                String staffDocument = normalizeDocument(staff.rut());
                if (normalizedUserDocument != null && normalizedUserDocument.equals(staffDocument)) {
                    return true;
                }

                String staffEmail = normalizeEmail(staff.email());
                if (normalizedUserEmail != null && normalizedUserEmail.equals(staffEmail)) {
                    return true;
                }

                String staffFirstName = normalizeText(staff.firstName());
                String staffLastName = normalizeText(staff.lastName());
                return normalizedFirstName != null
                    && normalizedLastName != null
                    && normalizedFirstName.equals(staffFirstName)
                    && normalizedLastName.equals(staffLastName);
            })
            .findFirst();
    }

    private String normalizeDocument(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replace(".", "")
            .replace("-", "")
            .replace(" ", "")
            .trim()
            .toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
