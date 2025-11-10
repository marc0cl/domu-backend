package com.domu.backend.services;

import com.domu.backend.domain.Community;
import com.domu.backend.domain.Resident;
import com.domu.backend.domain.Role;
import com.domu.backend.domain.Unit;
import com.domu.backend.dto.ResidentRequest;
import com.domu.backend.dto.RoleAssignmentRequest;
import com.domu.backend.exceptions.ResourceNotFoundException;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.ResidentRepository;
import com.domu.backend.repository.RoleRepository;
import com.domu.backend.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ResidentService {

    private final ResidentRepository residentRepository;
    private final CommunityRepository communityRepository;
    private final UnitRepository unitRepository;
    private final RoleRepository roleRepository;

    public ResidentService(ResidentRepository residentRepository,
                           CommunityRepository communityRepository,
                           UnitRepository unitRepository,
                           RoleRepository roleRepository) {
        this.residentRepository = residentRepository;
        this.communityRepository = communityRepository;
        this.unitRepository = unitRepository;
        this.roleRepository = roleRepository;
    }

    public Resident createResident(ResidentRequest request) {
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

    public List<Resident> listResidents() {
        return residentRepository.findAll();
    }

    @Transactional
    public Resident assignRoles(RoleAssignmentRequest request) {
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
