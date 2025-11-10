package com.domu.backend.interfaces.http;

import com.domu.backend.config.AppConfig;
import com.domu.backend.infrastructure.persistence.DataSourceFactory;
import com.domu.backend.infrastructure.persistence.UserRepository;
import com.domu.backend.dto.ErrorResponse;
import com.domu.backend.infrastructure.security.AuthenticationHandler;
import com.domu.backend.infrastructure.security.BCryptPasswordHasher;
import com.domu.backend.infrastructure.security.JwtProvider;
import com.domu.backend.infrastructure.security.PasswordHasher;
import com.domu.backend.service.InvalidCredentialsException;
import com.domu.backend.service.UserAlreadyExistsException;
import com.domu.backend.service.UserService;
import com.domu.backend.service.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

public final class WebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

    private final AppConfig config;
    private final HikariDataSource dataSource;
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final AuthenticationHandler authenticationHandler;
    private final AuthController authController;
    private final UserController userController;
    private final Javalin app;

    public WebServer(AppConfig config) {
        this.config = config;
        this.dataSource = DataSourceFactory.create(config);
        PasswordHasher passwordHasher = new BCryptPasswordHasher();
        UserRepository userRepository = new UserRepository(dataSource);
        this.userService = new UserService(userRepository, passwordHasher);
        this.jwtProvider = new JwtProvider(
                config.jwtSecret(),
                config.jwtIssuer(),
                config.jwtExpirationMinutes()
        );
        this.authenticationHandler = new AuthenticationHandler(jwtProvider, userService);
        this.authController = new AuthController(userService, jwtProvider);
        this.userController = new UserController();
        this.app = createApp();
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        app.start(config.serverPort());
        LOGGER.info("Server started on port {}", config.serverPort());
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
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin javalin = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper));
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
        javalin.routes(() -> ApiBuilder.path("/api", () -> {
            ApiBuilder.path("/auth", () -> {
                ApiBuilder.post("/register", authController::register);
                ApiBuilder.post("/login", authController::login);
            });

            ApiBuilder.before("/users/*", authenticationHandler);

            ApiBuilder.get("/users/me", userController::currentUser);
        }));
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

}
