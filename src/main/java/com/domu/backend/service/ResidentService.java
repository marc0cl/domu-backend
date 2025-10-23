package com.domu.backend.service;

import com.domu.backend.domain.core.User;
import com.domu.backend.infrastructure.persistence.repository.HousingUnitRepository;

public class ResidentService {

    private final HousingUnitRepository housingUnitRepository;
    private final UserService userService;

    public ResidentService(HousingUnitRepository housingUnitRepository, UserService userService) {
        this.housingUnitRepository = housingUnitRepository;
        this.userService = userService;
    }

    public User registerResident(Long unitId,
                                 Long roleId,
                                 String firstName,
                                 String lastName,
                                 String email,
                                 String phone,
                                 String documentNumber,
                                 String rawPassword) {
        if (unitId != null) {
            housingUnitRepository.findById(unitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Housing unit not found"));
        }
        return userService.registerUser(
                unitId,
                roleId,
                firstName,
                lastName,
                null,
                email,
                phone,
                documentNumber,
                Boolean.TRUE,
                rawPassword
        );
    }

    public User requireResident(Long userId) {
        return userService.findById(userId)
                .filter(user -> Boolean.TRUE.equals(user.resident()))
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
    }
}
