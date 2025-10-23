package com.domu.backend.interfaces.http;

import com.domu.backend.dto.UserResponse;

import io.javalin.http.Context;

public class UserController {

    public void currentUser(Context ctx) {
        UserResponse response = UserMapper.toResponseFromContext(ctx);
        ctx.json(response);
    }
}
