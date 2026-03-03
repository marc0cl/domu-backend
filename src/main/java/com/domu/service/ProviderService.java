package com.domu.service;

import com.domu.database.ProviderRepository;
import com.domu.dto.ProviderRequest;
import com.google.inject.Inject;

import java.util.List;
import java.util.Optional;

public class ProviderService {

    private final ProviderRepository repository;

    @Inject
    public ProviderService(ProviderRepository repository) {
        this.repository = repository;
    }

    public List<ProviderRepository.ProviderResponse> listByBuilding(Long buildingId) {
        return repository.findByBuilding(buildingId);
    }

    public List<ProviderRepository.ProviderResponse> listActiveByBuilding(Long buildingId) {
        return repository.findActiveByBuilding(buildingId);
    }

    public ProviderRepository.ProviderResponse create(ProviderRequest request) {
        repository.findByRut(request.rut()).ifPresent(existing -> {
            if (existing.buildingId().equals(request.buildingId())) {
                throw new RuntimeException("Ya existe un proveedor con este RUT en esta comunidad: " + request.rut());
            }
        });
        return repository.insert(request);
    }

    public ProviderRepository.ProviderResponse update(Long id, ProviderRequest request) {
        repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Proveedor no encontrado: " + id));

        repository.findByRut(request.rut()).ifPresent(existing -> {
            if (!existing.id().equals(id) && existing.buildingId().equals(request.buildingId())) {
                throw new RuntimeException("Ya existe otro proveedor con este RUT en esta comunidad: " + request.rut());
            }
        });

        return repository.update(id, request);
    }

    public void delete(Long id) {
        repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Proveedor no encontrado: " + id));
        repository.delete(id);
    }

    public ProviderRepository.ProviderResponse findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Proveedor no encontrado: " + id));
    }

    public Optional<ProviderRepository.ProviderResponse> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }
}
