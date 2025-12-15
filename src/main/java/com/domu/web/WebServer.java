package com.domu.web;

import com.domu.domain.core.User;
import com.domu.dto.ApproveBuildingRequest;
import com.domu.dto.AuthResponse;
import com.domu.dto.BuildingRequestResponse;
import com.domu.dto.CreateBuildingRequest;
import com.domu.dto.ErrorResponse;
import com.domu.dto.LoginRequest;
import com.domu.dto.RegistrationRequest;
import com.domu.dto.UserResponse;
import com.domu.dto.AddCommonChargesRequest;
import com.domu.dto.CommonPaymentRequest;
import com.domu.dto.CreateCommonExpensePeriodRequest;
import com.domu.dto.CreateVisitRequest;
import com.domu.dto.VisitContactRequest;
import com.domu.dto.VisitFromContactRequest;
import com.domu.dto.BuildingSummaryResponse;
import com.domu.dto.IncidentRequest;
import com.domu.service.BuildingService;
import com.domu.service.CommonExpenseService;
import com.domu.service.VisitService;
import com.domu.service.VisitContactService;
import com.domu.service.IncidentService;
import com.domu.security.AuthenticationHandler;
import com.domu.security.JwtProvider;
import com.domu.service.InvalidCredentialsException;
import com.domu.service.UserAlreadyExistsException;
import com.domu.service.UserService;
import com.domu.service.ValidationException;
import com.domu.database.UserBuildingRepository;
import com.domu.database.BuildingRepository;
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
    private final CommonExpenseService commonExpenseService;
    private final BuildingService buildingService;
    private final VisitService visitService;
    private final VisitContactService visitContactService;
    private final IncidentService incidentService;
    private final AuthenticationHandler authenticationHandler;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final UserBuildingRepository userBuildingRepository;
    private final BuildingRepository buildingRepository;
    private final Javalin app;
    private Integer port = -1;

    @Inject
    public WebServer(
        final HikariDataSource dataSource,
        final UserService userService,
        final CommonExpenseService commonExpenseService,
        final BuildingService buildingService,
        final VisitService visitService,
        final VisitContactService visitContactService,
        final IncidentService incidentService,
        final AuthenticationHandler authenticationHandler,
        final JwtProvider jwtProvider,
        final ObjectMapper objectMapper,
        final UserBuildingRepository userBuildingRepository,
        final BuildingRepository buildingRepository
    ) {
        this.dataSource = dataSource;
        this.userService = userService;
        this.commonExpenseService = commonExpenseService;
        this.buildingService = buildingService;
        this.visitService = visitService;
        this.visitContactService = visitContactService;
        this.incidentService = incidentService;
        this.authenticationHandler = authenticationHandler;
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
        this.userBuildingRepository = userBuildingRepository;
        this.buildingRepository = buildingRepository;
        this.app = createApp();
    }

    public void start(final Integer port) {
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
            var buildings = loadBuildings(created);
            Long activeBuildingId = resolveActiveBuildingId(created, buildings);
            ctx.json(UserMapper.toResponse(created, buildings, activeBuildingId));
        });

        javalin.post("/api/auth/login", ctx -> {
            LoginRequest request = validateLogin(ctx.bodyValidator(LoginRequest.class));
            User user = userService.authenticate(request.getEmail(), request.getPassword());
            String token = jwtProvider.generateToken(user);
            var buildings = loadBuildings(user);
            Long activeBuildingId = resolveActiveBuildingId(user, buildings);
            ctx.json(new AuthResponse(token, UserMapper.toResponse(user, buildings, activeBuildingId)));
        });

        javalin.before("/api/users/*", authenticationHandler);
        javalin.before("/api/finance/*", authenticationHandler);
        javalin.before("/api/buildings/*", authenticationHandler);
        javalin.before("/api/visits", authenticationHandler);
        javalin.before("/api/visits/*", authenticationHandler);
        javalin.before("/api/visit-contacts", authenticationHandler);
        javalin.before("/api/visit-contacts/*", authenticationHandler);
        javalin.before("/api/incidents", authenticationHandler);
        javalin.before("/api/incidents/*", authenticationHandler);

        javalin.get("/api/users/me", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            var buildings = loadBuildings(user);
            Long activeBuildingId = resolveActiveBuildingId(user, buildings);
            UserResponse response = UserMapper.toResponseFromContext(ctx, buildings, activeBuildingId);
            ctx.json(response);
        });

        javalin.post("/api/finance/periods", ctx -> {
            CreateCommonExpensePeriodRequest request = validateCreatePeriod(ctx.bodyValidator(CreateCommonExpensePeriodRequest.class));
            ctx.status(HttpStatus.CREATED);
            ctx.json(commonExpenseService.createPeriod(request));
        });

        javalin.post("/api/finance/periods/{periodId}/charges", ctx -> {
            Long periodId = Long.parseLong(ctx.pathParam("periodId"));
            AddCommonChargesRequest request = validateAddCharges(ctx.bodyValidator(AddCommonChargesRequest.class));
            ctx.json(commonExpenseService.addCharges(periodId, request));
        });

        javalin.get("/api/finance/my-charges", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.json(commonExpenseService.getChargesForUser(user));
        });

        javalin.post("/api/finance/charges/{chargeId}/pay", ctx -> {
            Long chargeId = Long.parseLong(ctx.pathParam("chargeId"));
            CommonPaymentRequest request = validatePayment(ctx.bodyValidator(CommonPaymentRequest.class));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.json(commonExpenseService.payCharge(chargeId, user, request));
        });

        javalin.post("/api/buildings/requests", ctx -> {
            CreateBuildingRequest request = validateCreateBuilding(ctx.bodyValidator(CreateBuildingRequest.class));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.status(HttpStatus.CREATED);
            BuildingRequestResponse response = buildingService.createRequest(request, user);
            ctx.json(response);
        });

        javalin.post("/api/buildings/requests/{requestId}/approve", ctx -> {
            Long requestId = Long.parseLong(ctx.pathParam("requestId"));
            ApproveBuildingRequest request = validateApproveBuilding(ctx.bodyValidator(ApproveBuildingRequest.class));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.json(buildingService.approve(requestId, request, user));
        });

        javalin.post("/api/visits", ctx -> {
            CreateVisitRequest request = validateCreateVisit(ctx.bodyValidator(CreateVisitRequest.class));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.status(HttpStatus.CREATED);
            ctx.json(visitService.createVisit(user, request));
        });

        javalin.get("/api/visits/my", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.json(visitService.getVisitsForUser(user));
        });

        javalin.get("/api/visits/history", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            String search = ctx.queryParam("q");
            ctx.json(visitService.getVisitHistory(user, search));
        });

        javalin.post("/api/visit-contacts", ctx -> {
            VisitContactRequest request = validateVisitContact(ctx.bodyValidator(VisitContactRequest.class));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.status(HttpStatus.CREATED);
            ctx.json(visitContactService.create(user, request));
        });

        javalin.get("/api/visit-contacts", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            String search = ctx.queryParam("q");
            Integer limit = null;
            try {
                String rawLimit = ctx.queryParam("limit");
                if (rawLimit != null && !rawLimit.isBlank()) {
                    limit = Integer.parseInt(rawLimit);
                }
            } catch (NumberFormatException ignored) {
                // si no es número, dejamos limit en null
            }
            ctx.json(visitContactService.list(user, search, limit));
        });

        javalin.delete("/api/visit-contacts/{contactId}", ctx -> {
            Long contactId = Long.parseLong(ctx.pathParam("contactId"));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            visitContactService.delete(user, contactId);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        javalin.post("/api/visit-contacts/{contactId}/register", ctx -> {
            Long contactId = Long.parseLong(ctx.pathParam("contactId"));
            VisitFromContactRequest request = validateVisitFromContact(ctx.bodyValidator(VisitFromContactRequest.class));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.status(HttpStatus.CREATED);
            ctx.json(visitContactService.registerFromContact(user, contactId, request));
        });

        javalin.post("/api/visits/{authorizationId}/check-in", ctx -> {
            Long authorizationId = Long.parseLong(ctx.pathParam("authorizationId"));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.json(visitService.registerCheckIn(authorizationId, user));
        });

        javalin.get("/api/incidents/my", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            String from = ctx.queryParam("from");
            String to = ctx.queryParam("to");
            ctx.json(incidentService.list(user, parseDate(from), parseDate(to)));
        });

        javalin.post("/api/incidents", ctx -> {
            IncidentRequest request = validateIncident(ctx.bodyValidator(IncidentRequest.class));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.status(HttpStatus.CREATED);
            ctx.json(incidentService.create(user, request));
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

    private CreateCommonExpensePeriodRequest validateCreatePeriod(BodyValidator<CreateCommonExpensePeriodRequest> validator) {
        return validator
                .check(req -> req.getBuildingId() != null, "buildingId es requerido")
                .check(req -> req.getYear() != null, "year es requerido")
                .check(req -> req.getMonth() != null, "month es requerido")
                .check(req -> req.getDueDate() != null, "dueDate es requerido")
                .get();
    }

    private AddCommonChargesRequest validateAddCharges(BodyValidator<AddCommonChargesRequest> validator) {
        return validator
                .check(req -> req.getCharges() != null && !req.getCharges().isEmpty(), "Debe existir al menos un cargo")
                .get();
    }

    private CommonPaymentRequest validatePayment(BodyValidator<CommonPaymentRequest> validator) {
        return validator
                .check(req -> req.getAmount() != null, "amount es requerido")
                .check(req -> req.getPaymentMethod() != null && !req.getPaymentMethod().isBlank(), "paymentMethod es requerido")
                .get();
    }

    private CreateBuildingRequest validateCreateBuilding(BodyValidator<CreateBuildingRequest> validator) {
        return validator
                .check(req -> req.getName() != null && !req.getName().isBlank(), "name es requerido")
                .check(req -> req.getAddress() != null && !req.getAddress().isBlank(), "address es requerido")
                .check(req -> req.getProofText() != null && !req.getProofText().isBlank(), "proofText es requerido")
                .get();
    }

    private ApproveBuildingRequest validateApproveBuilding(BodyValidator<ApproveBuildingRequest> validator) {
        return validator.get();
    }

    private CreateVisitRequest validateCreateVisit(BodyValidator<CreateVisitRequest> validator) {
        return validator
                .check(req -> req.getVisitorName() != null && !req.getVisitorName().isBlank(), "visitorName es requerido")
                .get();
    }

    private VisitContactRequest validateVisitContact(BodyValidator<VisitContactRequest> validator) {
        return validator
                .check(req -> req.getVisitorName() != null && !req.getVisitorName().isBlank(), "visitorName es requerido")
                .get();
    }

    private VisitFromContactRequest validateVisitFromContact(BodyValidator<VisitFromContactRequest> validator) {
        return validator.get();
    }

    private java.util.List<BuildingSummaryResponse> loadBuildings(User user) {
        if (user == null) {
            return java.util.List.of();
        }
        return userBuildingRepository.findBuildingsForUser(user.id());
    }

    private Long resolveActiveBuildingId(User user, java.util.List<BuildingSummaryResponse> buildings) {
        if (user != null && user.unitId() != null) {
            Long buildingId = buildingRepository.findBuildingIdByUnitId(user.unitId());
            if (buildingId != null) {
                return buildingId;
            }
        }
        if (buildings != null && !buildings.isEmpty()) {
            return buildings.get(0).id();
        }
        return null;
    }

    private IncidentRequest validateIncident(BodyValidator<IncidentRequest> validator) {
        return validator
                .check(req -> req.getTitle() != null && !req.getTitle().isBlank(), "title es requerido")
                .check(req -> req.getDescription() != null && !req.getDescription().isBlank(), "description es requerido")
                .check(req -> req.getCategory() != null && !req.getCategory().isBlank(), "category es requerido")
                .get();
    }

    private java.time.LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    public Integer getPort() {
        return port;
    }
}
