package com.domu.web;

import com.domu.domain.core.User;
import com.domu.dto.BuildingSummaryResponse;
import com.domu.dto.UserResponse;
import com.domu.security.AuthenticationHandler;

import io.javalin.http.Context;

import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user, List<BuildingSummaryResponse> buildings, Long activeBuildingId) {
        return new UserResponse(
                user.id(),
                user.unitId(),
                user.roleId(),
                user.firstName(),
                user.lastName(),
                user.birthDate(),
                user.email(),
                user.phone(),
                user.documentNumber(),
                user.resident(),
                user.createdAt(),
                user.status(),
                activeBuildingId,
                buildings
        );
    }

    public static UserResponse toResponseFromContext(Context ctx, List<BuildingSummaryResponse> buildings, Long activeBuildingId) {
        User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
        if (user == null) {
            throw new IllegalStateException("Missing authenticated user in context");
        }
        return toResponse(user, buildings, activeBuildingId);
    }
}
