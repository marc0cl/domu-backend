package com.domu.backend.interfaces.http;

import com.domu.backend.domain.core.User;
import com.domu.backend.dto.AuthResponse;
import com.domu.backend.dto.LoginRequest;
import com.domu.backend.dto.RegistrationRequest;
import com.domu.backend.infrastructure.security.JwtProvider;
import com.domu.backend.service.UserService;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.validation.BodyValidator;

public class AuthController {

    private final UserService userService;
    private final JwtProvider jwtProvider;

    public AuthController(UserService userService, JwtProvider jwtProvider) {
        this.userService = userService;
        this.jwtProvider = jwtProvider;
    }

    public void register(Context ctx) {
        RegistrationRequest request = validateRegistration(ctx.bodyValidator(RegistrationRequest.class));
        User created = userService.registerUser(
                request.getUnitId(),
                request.getRoleId(),
                request.getFirstName(),
                request.getLastName(),
                request.getBirthDate(),
                request.getEmail(),
                request.getPhone(),
                request.getDocumentNumber(),
                request.getResident(),
                request.getPassword()
        );
        ctx.status(HttpStatus.CREATED);
        ctx.json(UserMapper.toResponse(created));
    }

    public void login(Context ctx) {
        LoginRequest request = validateLogin(ctx.bodyValidator(LoginRequest.class));
        User user = userService.authenticate(request.getEmail(), request.getPassword());
        String token = jwtProvider.generateToken(user);
        ctx.json(new AuthResponse(token, UserMapper.toResponse(user)));
    }

    private RegistrationRequest validateRegistration(BodyValidator<RegistrationRequest> validator) {
        return validator
                .check(req -> req.getFirstName() != null && !req.getFirstName().isBlank(), "firstName is required")
                .check(req -> req.getLastName() != null && !req.getLastName().isBlank(), "lastName is required")
                .check(req -> req.getEmail() != null && !req.getEmail().isBlank(), "email is required")
                .check(req -> req.getPassword() != null && req.getPassword().length() >= 10,
                        "password must contain at least 10 characters")
                .get();
    }

    private LoginRequest validateLogin(BodyValidator<LoginRequest> validator) {
        return validator
                .check(req -> req.getEmail() != null && !req.getEmail().isBlank(), "email is required")
                .check(req -> req.getPassword() != null && !req.getPassword().isBlank(), "password is required")
                .get();
    }
}
