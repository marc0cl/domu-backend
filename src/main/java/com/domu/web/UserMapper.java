package com.domu.web;

import com.domu.domain.core.User;
import com.domu.dto.BuildingSummaryResponse;
import com.domu.dto.UserResponse;
import com.domu.security.AuthenticationHandler;
import com.domu.service.BoxStorageService;

import io.javalin.http.Context;

import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user, List<BuildingSummaryResponse> buildings, Long activeBuildingId, BoxStorageService box) {
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
                resolveUrl(user.avatarBoxId(), box),
                resolveUrl(user.privacyAvatarBoxId(), box),
                user.displayName(),
                activeBuildingId,
                buildings
        );
    }

    public static UserResponse toResponseFromContext(Context ctx, List<BuildingSummaryResponse> buildings, Long activeBuildingId, BoxStorageService box) {
        User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
        if (user == null) {
            throw new IllegalStateException("Missing authenticated user in context");
        }
        return toResponse(user, buildings, activeBuildingId, box);
    }

    /**
     * If the stored value is a Box file ID (not a URL), generate a proxy URL.
     * If it's already a full URL (legacy), return as-is.
     * If null/blank, return null.
     */
    public static String resolveUrl(String storedValue, BoxStorageService box) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }
        if (box == null) {
            return storedValue;
        }
        // If it starts with "http", it's a legacy URL — return as-is
        if (storedValue.startsWith("http")) {
            return storedValue;
        }
        // Otherwise it's a Box file ID — generate proxy URL
        return box.resolveUrl(storedValue);
    }
}
