package com.domu.backend.controllers;

import com.domu.backend.domain.Permission;
import com.domu.backend.domain.Role;
import com.domu.backend.dto.PermissionAssignmentRequest;
import com.domu.backend.dto.PermissionRequest;
import com.domu.backend.dto.RoleRequest;
import com.domu.backend.services.RbacService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rbac")
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @PostMapping("/roles")
    public Role createRole(@Valid @RequestBody RoleRequest request) {
        return rbacService.createRole(request);
    }

    @GetMapping("/roles")
    public List<Role> listRoles() {
        return rbacService.listRoles();
    }

    @PostMapping("/permissions")
    public Permission createPermission(@Valid @RequestBody PermissionRequest request) {
        return rbacService.createPermission(request);
    }

    @GetMapping("/permissions")
    public List<Permission> listPermissions() {
        return rbacService.listPermissions();
    }

    @PostMapping("/roles/assign-permissions")
    @Transactional
    public Role assignPermissions(@Valid @RequestBody PermissionAssignmentRequest request) {
        return rbacService.assignPermissions(request);
    }
}
