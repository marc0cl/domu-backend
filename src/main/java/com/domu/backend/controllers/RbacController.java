package com.domu.backend.controllers;

import com.domu.backend.domain.Permission;
import com.domu.backend.domain.Role;
import com.domu.backend.dto.PermissionAssignmentRequest;
import com.domu.backend.dto.PermissionRequest;
import com.domu.backend.dto.RoleRequest;
import com.domu.backend.repository.PermissionRepository;
import com.domu.backend.repository.RoleRepository;
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
@RequestMapping("/api/rbac")
public class RbacController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RbacController(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @PostMapping("/roles")
    public Role createRole(@Valid @RequestBody RoleRequest request) {
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        return roleRepository.save(role);
    }

    @GetMapping("/roles")
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    @PostMapping("/permissions")
    public Permission createPermission(@Valid @RequestBody PermissionRequest request) {
        Permission permission = new Permission();
        permission.setCode(request.code());
        permission.setDescription(request.description());
        return permissionRepository.save(permission);
    }

    @GetMapping("/permissions")
    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }

    @PostMapping("/roles/assign-permissions")
    @Transactional
    public Role assignPermissions(@Valid @RequestBody PermissionAssignmentRequest request) {
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
