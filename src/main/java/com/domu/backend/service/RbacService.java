package com.domu.backend.service;

import com.domu.backend.domain.core.Role;
import com.domu.backend.infrastructure.persistence.repository.RoleRepository;

import java.util.List;

public class RbacService {

    private final RoleRepository roleRepository;

    public RbacService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role createRole(Role role) {
        return roleRepository.save(role);
    }

    public List<Role> listRoles() {
        return roleRepository.findAll();
    }
}
