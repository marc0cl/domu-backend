package com.domu.web;

import com.domu.domain.core.User;
import com.domu.dto.BuildingSummaryResponse;
import com.domu.dto.UserResponse;
import com.domu.security.AuthenticationHandler;
import com.domu.service.GcsStorageService;

import io.javalin.http.Context;

import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user, List<BuildingSummaryResponse> buildings, Long activeBuildingId, GcsStorageService gcs) {
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
                resolveUrl(user.avatarBoxId(), gcs),
                resolveUrl(user.privacyAvatarBoxId(), gcs),
                user.displayName(),
                activeBuildingId,
                buildings
        );
    }

    public static UserResponse toResponseFromContext(Context ctx, List<BuildingSummaryResponse> buildings, Long activeBuildingId, GcsStorageService gcs) {
        User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
        if (user == null) {
            throw new IllegalStateException("Missing authenticated user in context");
        }
        return toResponse(user, buildings, activeBuildingId, gcs);
    }

    /**
     * If the stored value looks like a GCS object path (no http), generate a fresh signed URL.
     * If it's already a full URL (legacy Box URLs or already signed), return as-is.
     * If null/blank, return null.
     */
    public static String resolveUrl(String storedValue, GcsStorageService gcs) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }
        if (gcs == null) {
            return storedValue;
        }
        // If it's a GCS object path (starts with "profiles/" etc.), sign it
        if (!storedValue.startsWith("http")) {
            return gcs.signedUrl(storedValue);
        }
        // If it's a storage.googleapis.com URL, extract path and re-sign
        if (storedValue.contains("storage.googleapis.com")) {
            // Already a GCS URL — might be unsigned or expired, try to extract path and re-sign
            String prefix = "storage.googleapis.com/";
            int idx = storedValue.indexOf(prefix);
            if (idx >= 0) {
                String rest = storedValue.substring(idx + prefix.length());
                // Remove bucket name prefix
                int slash = rest.indexOf('/');
                if (slash > 0) {
                    String objectPath = rest.substring(slash + 1);
                    int q = objectPath.indexOf('?');
                    if (q > 0) objectPath = objectPath.substring(0, q);
                    return gcs.signedUrl(objectPath);
                }
            }
        }
        // Legacy Box URLs or other — return as-is
        return storedValue;
    }
}
