package com.domu.backend.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.domu.backend.domain.User;
import com.domu.backend.service.UserService;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;

public class AuthenticationHandler implements Handler {

    public static final String USER_ATTRIBUTE = "authenticatedUser";

    private final JwtProvider jwtProvider;
    private final UserService userService;

    public AuthenticationHandler(JwtProvider jwtProvider, UserService userService) {
        this.jwtProvider = jwtProvider;
        this.userService = userService;
    }

    @Override
    public void handle(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Authorization header missing or invalid");
        }
        String token = header.substring("Bearer ".length());
        try {
            DecodedJWT decoded = jwtProvider.verify(token);
            Long userId = Long.parseLong(decoded.getSubject());
            User user = userService.findById(userId)
                    .orElseThrow(() -> new UnauthorizedResponse("User not found"));
            ctx.attribute(USER_ATTRIBUTE, user);
        } catch (JWTVerificationException | NumberFormatException ex) {
            throw new UnauthorizedResponse("Token inválido");
        }
    }
}
