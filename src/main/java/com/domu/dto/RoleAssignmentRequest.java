package com.domu.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record RoleAssignmentRequest(
        @NotNull Long residentId,
        @NotNull Set<Long> roleIds
) {
}
