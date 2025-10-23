package com.domu.backend.services;

import com.domu.backend.domain.Permission;
import com.domu.backend.domain.Role;
import com.domu.backend.dto.PermissionAssignmentRequest;
import com.domu.backend.dto.PermissionRequest;
import com.domu.backend.dto.RoleRequest;
import com.domu.backend.exceptions.ResourceNotFoundException;
import com.domu.backend.repository.PermissionRepository;
import com.domu.backend.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RbacService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public Role createRole(RoleRequest request) {
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        return roleRepository.save(role);
    }

    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    public Permission createPermission(PermissionRequest request) {
        Permission permission = new Permission();
        permission.setCode(request.code());
        permission.setDescription(request.description());
        return permissionRepository.save(permission);
    }

    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }

    @Transactional
    public Role assignPermissions(PermissionAssignmentRequest request) {
        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.permissionIds()));
        if (permissions.size() != request.permissionIds().size()) {
            throw new ResourceNotFoundException("One or more permissions not found");
        }
        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
        return roleRepository.save(role);
    }
}
