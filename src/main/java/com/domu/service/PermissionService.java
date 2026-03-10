package com.domu.service;

import com.domu.database.RoleRepository;
import com.domu.domain.core.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.javalin.http.ForbiddenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionService.class);
    private static final String WILDCARD = "ALL";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final RoleRepository roleRepository;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, List<String>> cache = new ConcurrentHashMap<>();

    @Inject
    public PermissionService(RoleRepository roleRepository, ObjectMapper objectMapper) {
        this.roleRepository = roleRepository;
        this.objectMapper = objectMapper;
    }

    public List<String> getPermissions(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return cache.computeIfAbsent(roleId, this::loadPermissions);
    }

    public boolean hasPermission(Long roleId, String permission) {
        List<String> permissions = getPermissions(roleId);
        return permissions.contains(WILDCARD) || permissions.contains(permission);
    }

    public void requirePermission(User user, String permission) {
        if (user == null) {
            throw new ForbiddenResponse("Autenticación requerida");
        }
        if (!hasPermission(user.roleId(), permission)) {
            throw new ForbiddenResponse("No tienes permiso para realizar esta acción");
        }
    }

    private List<String> loadPermissions(Long roleId) {
        return roleRepository.findPermissionsJsonByRoleId(roleId)
                .map(this::parseJson)
                .orElse(Collections.emptyList());
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return Collections.unmodifiableList(objectMapper.readValue(json, STRING_LIST));
        } catch (Exception e) {
            LOGGER.warn("Failed to parse permissions_json: {}", json, e);
            return Collections.emptyList();
        }
    }
}
