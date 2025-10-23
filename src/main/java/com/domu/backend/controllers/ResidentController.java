package com.domu.backend.controllers;

import com.domu.backend.domain.Community;
import com.domu.backend.domain.Resident;
import com.domu.backend.domain.Role;
import com.domu.backend.domain.Unit;
import com.domu.backend.dto.ResidentRequest;
import com.domu.backend.dto.RoleAssignmentRequest;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.ResidentRepository;
import com.domu.backend.repository.RoleRepository;
import com.domu.backend.repository.UnitRepository;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/residents")
public class ResidentController {

    private final ResidentRepository residentRepository;
    private final CommunityRepository communityRepository;
    private final UnitRepository unitRepository;
    private final RoleRepository roleRepository;

    public ResidentController(ResidentRepository residentRepository,
                              CommunityRepository communityRepository,
                              UnitRepository unitRepository,
                              RoleRepository roleRepository) {
        this.residentRepository = residentRepository;
        this.communityRepository = communityRepository;
        this.unitRepository = unitRepository;
        this.roleRepository = roleRepository;
    }

    @PostMapping
    public Resident createResident(@Valid @RequestBody ResidentRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Resident resident = new Resident();
        resident.setCommunity(community);
        if (request.unitId() != null) {
            Unit unit = unitRepository.findById(request.unitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            resident.setUnit(unit);
        }
        resident.setFirstName(request.firstName());
        resident.setLastName(request.lastName());
        resident.setEmail(request.email());
        resident.setPhone(request.phone());
        resident.setRut(request.rut());
        resident.setOwner(request.owner());
        resident.setActive(request.active());
        return residentRepository.save(resident);
    }

    @GetMapping
    public List<Resident> listResidents() {
        return residentRepository.findAll();
    }

    @PostMapping("/assign-roles")
    @Transactional
    public Resident assignRoles(@Valid @RequestBody RoleAssignmentRequest request) {
        Resident resident = residentRepository.findById(request.residentId())
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
        if (roles.size() != request.roleIds().size()) {
            throw new ResourceNotFoundException("One or more roles not found");
        }
        resident.getRoles().clear();
        resident.getRoles().addAll(roles);
        return residentRepository.save(resident);
    }
}
