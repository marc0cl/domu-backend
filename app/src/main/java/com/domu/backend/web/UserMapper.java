package com.domu.backend.web;

import com.domu.backend.domain.User;
import com.domu.backend.dto.UserResponse;
import com.domu.backend.security.AuthenticationHandler;

import io.javalin.http.Context;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.id(),
                user.unitId(),
                user.roleId(),
                user.firstName(),
                user.lastName(),
                user.birthDate(),
                user.email()
        );
    }

    public static UserResponse toResponseFromContext(Context ctx) {
        User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
        if (user == null) {
            throw new IllegalStateException("Missing authenticated user in context");
        }
        return toResponse(user);
    }
}
