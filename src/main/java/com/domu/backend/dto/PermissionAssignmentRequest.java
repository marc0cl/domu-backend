package com.domu.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record PermissionAssignmentRequest(
        @NotNull Long roleId,
        @NotNull Set<Long> permissionIds
) {
}
