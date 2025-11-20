package com.domu.backend.web;

import com.domu.backend.domain.core.User;
import com.domu.backend.dto.AuthResponse;
import com.domu.backend.dto.ErrorResponse;
import com.domu.backend.dto.LoginRequest;
import com.domu.backend.dto.RegistrationRequest;
import com.domu.backend.dto.UserResponse;
import com.domu.backend.security.AuthenticationHandler;
import com.domu.backend.security.JwtProvider;
import com.domu.backend.service.InvalidCredentialsException;
import com.domu.backend.service.UserAlreadyExistsException;
import com.domu.backend.service.UserService;
import com.domu.backend.service.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import io.javalin.validation.BodyValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

@Singleton
public final class WebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

    private final HikariDataSource dataSource;
    private final UserService userService;
    private final AuthenticationHandler authenticationHandler;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final Javalin app;
    private int port = -1;

    @Inject
    public WebServer(
        final HikariDataSource dataSource,
        final UserService userService,
        final AuthenticationHandler authenticationHandler,
        final JwtProvider jwtProvider,
        final ObjectMapper objectMapper
    ) {
        this.dataSource = dataSource;
        this.userService = userService;
        this.authenticationHandler = authenticationHandler;
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
        this.app = createApp();
    }

    public void start(final int port) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        app.start(port);
        this.port = port;
        LOGGER.info("Server started on port {}", port);
    }

    public void stop() {
        try {
            app.stop();
        } catch (IllegalStateException ignored) {
            // server was not running
        } finally {
            if (!dataSource.isClosed()) {
                dataSource.close();
            }
            LOGGER.info("Server stopped");
        }
    }

    private Javalin createApp() {
        Javalin javalin = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
            config.http.defaultContentType = "application/json";
            config.http.prefer405over404 = true;
            config.showJavalinBanner = false;
        });

        registerExceptionHandlers(javalin);
        registerRoutes(javalin);
        javalin.get("/health", ctx -> ctx.result("OK"));

        return javalin;
    }

    private void registerRoutes(Javalin javalin) {
        javalin.post("/api/auth/register", ctx -> {
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
        });

        javalin.post("/api/auth/login", ctx -> {
            LoginRequest request = validateLogin(ctx.bodyValidator(LoginRequest.class));
            User user = userService.authenticate(request.getEmail(), request.getPassword());
            String token = jwtProvider.generateToken(user);
            ctx.json(new AuthResponse(token, UserMapper.toResponse(user)));
        });

        javalin.before("/api/users/*", authenticationHandler);

        javalin.get("/api/users/me", ctx -> {
            UserResponse response = UserMapper.toResponseFromContext(ctx);
            ctx.json(response);
        });
    }

    private void registerExceptionHandlers(Javalin javalin) {
        javalin.exception(UserAlreadyExistsException.class, (exception, ctx) -> {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(ErrorResponse.of(exception.getMessage(), HttpStatus.CONFLICT.getCode()));
        });
        javalin.exception(ValidationException.class, (exception, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(ErrorResponse.of(exception.getMessage(), HttpStatus.BAD_REQUEST.getCode()));
        });
        javalin.exception(InvalidCredentialsException.class, (exception, ctx) -> {
            ctx.status(HttpStatus.UNAUTHORIZED);
            ctx.json(ErrorResponse.of(exception.getMessage(), HttpStatus.UNAUTHORIZED.getCode()));
        });
        javalin.exception(Exception.class, (exception, ctx) -> {
            LOGGER.error("Unexpected error", exception);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(ErrorResponse.of("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR.getCode()));
        });
    }

    private RegistrationRequest validateRegistration(BodyValidator<RegistrationRequest> validator) {
        return validator
                .check(req -> req.getFirstName() != null && !req.getFirstName().isBlank(), "firstName is required")
                .check(req -> req.getLastName() != null && !req.getLastName().isBlank(), "lastName is required")
                .check(req -> req.getPhone() != null && !req.getPhone().isBlank(), "phone is required")
                .check(req -> req.getDocumentNumber() != null && !req.getDocumentNumber().isBlank(), "documentNumber is required")
                .check(req -> req.getResident() != null, "resident is required")
                .check(req -> req.getEmail() != null && !req.getEmail().isBlank(), "email is required")
                .check(req -> req.getPassword() != null && req.getPassword().length() >= 10, "password must contain at least 10 characters")
                .get();
    }

    private LoginRequest validateLogin(BodyValidator<LoginRequest> validator) {
        return validator
                .check(req -> req.getEmail() != null && !req.getEmail().isBlank(), "email is required")
                .check(req -> req.getPassword() != null && !req.getPassword().isBlank(), "password is required")
                .get();
    }

    public int getPort() {
        return port;
    }
}
