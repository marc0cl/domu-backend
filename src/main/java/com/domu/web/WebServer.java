package com.domu.web;

import com.domu.domain.BuildingRequest;
import com.domu.domain.core.User;
import com.domu.dto.ApproveBuildingRequest;
import com.domu.dto.AdminInviteInfoResponse;
import com.domu.dto.AdminInviteRegistrationRequest;
import com.domu.dto.AuthResponse;
import com.domu.dto.BuildingRequestResponse;
import com.domu.dto.ConfirmationRequest;
import com.domu.dto.CreateBuildingRequest;
import com.domu.dto.ErrorResponse;
import com.domu.dto.LoginRequest;
import com.domu.dto.MarketItemRequest;
import com.domu.dto.MarketItemResponse;
import com.domu.dto.ChatMessageRequest;
import com.domu.dto.ChatMessageResponse;
import com.domu.dto.ChatRoomResponse;
import com.domu.dto.ChatRequestResponse;
import com.domu.dto.UserProfileResponse;
import com.domu.dto.RegistrationRequest;
import com.domu.dto.UserResponse;
import com.domu.service.ChatRequestService;
import com.domu.service.UserProfileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.domu.dto.AddCommonChargesRequest;
import com.domu.dto.CommonPaymentRequest;
import com.domu.dto.CreateCommonExpensePeriodRequest;
import com.domu.dto.CreateVisitRequest;
import com.domu.dto.VisitContactRequest;
import com.domu.dto.VisitFromContactRequest;
import com.domu.dto.VisitorQrRequest;
import com.domu.dto.BuildingSummaryResponse;
import com.domu.dto.IncidentAssignmentRequest;
import com.domu.dto.IncidentRequest;
import com.domu.dto.CommunityRegistrationDocument;
import com.domu.dto.UpdateProfileRequest;
import com.domu.dto.ChangePasswordRequest;
import com.domu.dto.IncidentStatusUpdateRequest;
import com.domu.dto.ParcelRequest;
import com.domu.dto.ParcelStatusUpdateRequest;
import com.domu.dto.UnitSummaryResponse;
import com.domu.dto.CreatePollRequest;
import com.domu.dto.VoteRequest;
import com.domu.dto.AmenityRequest;
import com.domu.dto.TimeSlotRequest;
import com.domu.dto.ReservationRequest;
import com.domu.service.AmenityService;
import com.domu.service.MarketService;
import com.domu.service.ChatService;
import com.domu.service.BuildingService;
import com.domu.service.CommonExpensePdfService;
import com.domu.service.CommonExpenseService;
import com.domu.service.PaymentReceiptPdfService;
import com.domu.service.ChargeReceiptPdfService;
import com.domu.dto.SimulatedPaymentRequest;
import com.domu.service.VisitService;
import com.domu.service.VisitContactService;
import com.domu.service.IncidentService;
import com.domu.service.PollService;
import com.domu.service.ParcelService;
import com.domu.service.TaskService;
import com.domu.service.StaffService;
import com.domu.database.TaskRepository;
import com.domu.dto.TaskRequest;
import com.domu.dto.StaffRequest;
import com.domu.service.ChatRequestService;
import com.domu.service.UserProfileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.domu.security.AuthenticationHandler;
import com.domu.security.JwtProvider;
import com.domu.service.InvalidCredentialsException;
import com.domu.service.UserAlreadyExistsException;
import com.domu.service.UserService;
import com.domu.service.ValidationException;
import com.domu.database.HousingUnitRepository;
import com.domu.domain.core.HousingUnit;
import com.domu.service.ForumService;
import com.domu.dto.CreateThreadRequest;
import com.domu.database.UserBuildingRepository;
import com.domu.database.BuildingRepository;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.json.JavalinJackson;
import io.javalin.http.UploadedFile;
import io.javalin.validation.BodyValidator;
import io.javalin.http.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.domu.email.EmailService;

@Singleton
public final class WebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

    private final HikariDataSource dataSource;
    private final UserService userService;
    private final CommonExpenseService commonExpenseService;
    private final CommonExpensePdfService commonExpensePdfService;
    private final PaymentReceiptPdfService paymentReceiptPdfService;
    private final ChargeReceiptPdfService chargeReceiptPdfService;
    private final BuildingService buildingService;
    private final VisitService visitService;
    private final VisitContactService visitContactService;
    private final IncidentService incidentService;
    private final PollService pollService;
    private final AmenityService amenityService;
    private final MarketService marketService;
    private final ChatService chatService;
    private final ChatRequestService chatRequestService;
    private final UserProfileService userProfileService;
    private final ParcelService parcelService;
    private final TaskService taskService;
    private final StaffService staffService;
    private final com.domu.service.LibraryService libraryService;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final com.domu.service.HousingUnitService housingUnitService;
    private final AuthenticationHandler authenticationHandler;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final UserBuildingRepository userBuildingRepository;
    private final BuildingRepository buildingRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final EmailService emailService;
    private final com.domu.database.UserRepository userRepository;
    private final ForumService forumService;
    private final com.domu.service.GcsStorageService gcsStorageService;
    private final Javalin app;
    private Integer port = -1;

    @Inject
    public WebServer(
            final HikariDataSource dataSource,
            final UserService userService,
            final CommonExpenseService commonExpenseService,
            final CommonExpensePdfService commonExpensePdfService,
            final PaymentReceiptPdfService paymentReceiptPdfService,
            final ChargeReceiptPdfService chargeReceiptPdfService,
            final BuildingService buildingService,
            final VisitService visitService,
            final VisitContactService visitContactService,
            final IncidentService incidentService,
            final PollService pollService,
            final AmenityService amenityService,
            final MarketService marketService,
            final ChatService chatService,
            final ChatRequestService chatRequestService,
            final UserProfileService userProfileService,
            final ParcelService parcelService,
            final TaskService taskService,
            final StaffService staffService,
            final com.domu.service.LibraryService libraryService,
            final ChatWebSocketHandler chatWebSocketHandler,
            final com.domu.service.HousingUnitService housingUnitService,
            final AuthenticationHandler authenticationHandler,
            final JwtProvider jwtProvider,
            final ObjectMapper objectMapper,
            final UserBuildingRepository userBuildingRepository,
            final BuildingRepository buildingRepository,
            final HousingUnitRepository housingUnitRepository,
            final EmailService emailService,
            final com.domu.database.UserRepository userRepository,
            final ForumService forumService,
            final com.domu.service.GcsStorageService gcsStorageService) {
        this.dataSource = dataSource;
        this.userService = userService;
        this.commonExpenseService = commonExpenseService;
        this.commonExpensePdfService = commonExpensePdfService;
        this.paymentReceiptPdfService = paymentReceiptPdfService;
        this.chargeReceiptPdfService = chargeReceiptPdfService;
        this.buildingService = buildingService;
        this.visitService = visitService;
        this.visitContactService = visitContactService;
        this.incidentService = incidentService;
        this.pollService = pollService;
        this.amenityService = amenityService;
        this.marketService = marketService;
        this.chatService = chatService;
        this.chatRequestService = chatRequestService;
        this.userProfileService = userProfileService;
        this.parcelService = parcelService;
        this.taskService = taskService;
        this.staffService = staffService;
        this.libraryService = libraryService;
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.housingUnitService = housingUnitService;
        this.authenticationHandler = authenticationHandler;
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
        this.userBuildingRepository = userBuildingRepository;
        this.buildingRepository = buildingRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.forumService = forumService;
        this.gcsStorageService = gcsStorageService;
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
        javalin.ws("/ws/chat", chatWebSocketHandler::handle);
        javalin.get("/health", ctx -> ctx.result("OK"));

        return javalin;
    }

    private void registerRoutes(Javalin javalin) {
        javalin.get("/aprobar-solicitud", ctx -> {
            String code = ctx.queryParam("code");
            try {
                BuildingRequest request = buildingService.approveByCode(code);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderApprovalResultPage(
                        true,
                        "Solicitud aprobada",
                        "La solicitud de " + escapeHtml(request.name())
                                + " fue aprobada y notificamos al solicitante."));
            } catch (ValidationException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderApprovalResultPage(false, "Enlace inválido", e.getMessage()));
            } catch (Exception e) {
                LOGGER.error("Error procesando aprobación por código", e);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderApprovalResultPage(false, "Error inesperado",
                        "Ocurrió un problema al procesar el enlace. Intenta nuevamente."));
            }
        });

        javalin.get("/rechazar-solicitud", ctx -> {
            String code = ctx.queryParam("code");
            try {
                BuildingRequest request = buildingService.validateApprovalLink(code);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderRejectionForm(code, request.name()));
            } catch (ValidationException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderApprovalResultPage(false, "Enlace inválido", e.getMessage()));
            } catch (Exception e) {
                LOGGER.error("Error mostrando formulario de rechazo por código", e);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderApprovalResultPage(false, "Error inesperado",
                        "Ocurrió un problema al procesar el enlace. Intenta nuevamente."));
            }
        });

        javalin.post("/rechazar-solicitud", ctx -> {
            String code = ctx.formParam("code");
            String reason = ctx.formParam("reason");
            try {
                BuildingRequest request = buildingService.rejectByCode(code, reason);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderApprovalResultPage(
                        true,
                        "Solicitud rechazada",
                        "Registramos el rechazo para " + escapeHtml(request.name())
                                + " y el solicitante fue notificado."));
            } catch (ValidationException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderApprovalResultPage(false, "No pudimos rechazar", e.getMessage()));
            } catch (Exception e) {
                LOGGER.error("Error procesando rechazo por código", e);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderApprovalResultPage(false, "Error inesperado",
                        "Ocurrió un problema al registrar el rechazo. Intenta nuevamente."));
            }
        });

        javalin.get("/registrar-admin", ctx -> {
            String code = ctx.queryParam("code");
            if (code == null || code.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderAdminInviteResult(false, "Enlace inválido", "Falta el código de invitación."));
                return;
            }
            String frontendBaseUrl = resolveFrontendBaseUrl();
            String targetUrl = frontendBaseUrl + "/registrar-admin?code="
                    + URLEncoder.encode(code, StandardCharsets.UTF_8);
            ctx.redirect(targetUrl);
        });

        javalin.post("/registrar-admin", ctx -> {
            String code = ctx.formParam("code");
            String firstName = ctx.formParam("firstName");
            String lastName = ctx.formParam("lastName");
            String phone = ctx.formParam("phone");
            String documentNumber = ctx.formParam("documentNumber");
            String password = ctx.formParam("password");
            try {
                buildingService.registerAdminFromInvite(code, firstName, lastName, phone, documentNumber, password);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderAdminInviteResult(true, "Cuenta creada",
                        "Ya puedes iniciar sesión con tu correo y la contraseña que definiste."));
            } catch (ValidationException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderAdminInviteResult(false, "No pudimos crear tu cuenta", e.getMessage()));
            } catch (Exception e) {
                LOGGER.error("Error registrando administrador desde invitación", e);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.contentType("text/html; charset=UTF-8");
                ctx.result(renderAdminInviteResult(false, "Error inesperado",
                        "Ocurrió un problema al crear tu cuenta. Intenta nuevamente."));
            }
        });

        javalin.get("/api/admin-invites/{code}", ctx -> {
            String code = ctx.pathParam("code");
            try {
                AdminInviteInfoResponse invite = buildingService.getAdminInviteInfo(code);
                ctx.json(invite);
            } catch (ValidationException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of(e.getMessage(), HttpStatus.BAD_REQUEST.getCode()));
            } catch (Exception e) {
                LOGGER.error("Error obteniendo datos de invitación de admin", e);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(ErrorResponse.of("Ocurrió un problema al procesar el enlace. Intenta nuevamente.",
                        HttpStatus.INTERNAL_SERVER_ERROR.getCode()));
            }
        });

        javalin.post("/api/admin-invites/{code}", ctx -> {
            String code = ctx.pathParam("code");
            AdminInviteRegistrationRequest request = ctx.bodyValidator(AdminInviteRegistrationRequest.class)
                    .check(body -> body.getPassword() != null && body.getPassword().length() >= 10,
                            "La contraseña debe tener al menos 10 caracteres")
                    .get();
            try {
                buildingService.registerAdminFromInvite(
                        code,
                        request.getFirstName(),
                        request.getLastName(),
                        request.getPhone(),
                        request.getDocumentNumber(),
                        request.getPassword());
                ctx.status(HttpStatus.CREATED);
                ctx.json(java.util.Map.of(
                        "message",
                        "Cuenta creada. Ya puedes iniciar sesión con tu correo y la contraseña que definiste."));
            } catch (ValidationException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of(e.getMessage(), HttpStatus.BAD_REQUEST.getCode()));
            } catch (Exception e) {
                LOGGER.error("Error registrando administrador desde invitación (API)", e);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(ErrorResponse.of("Ocurrió un problema al crear tu cuenta. Intenta nuevamente.",
                        HttpStatus.INTERNAL_SERVER_ERROR.getCode()));
            }
        });

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
                    request.getPassword());
            ctx.status(HttpStatus.CREATED);
            var buildings = loadBuildings(created);
            Long activeBuildingId = resolveActiveBuildingId(created, buildings);
            ctx.json(UserMapper.toResponse(created, buildings, activeBuildingId, gcsStorageService));
        });

        javalin.post("/api/auth/login", ctx -> {
            LoginRequest request = ctx.bodyValidator(LoginRequest.class).get();
            User user = userService.authenticate(request.getEmail(), request.getPassword());
            String token = jwtProvider.generateToken(user);
            var buildings = loadBuildings(user);
            Long activeBuildingId = resolveActiveBuildingId(user, buildings);
            ctx.json(new AuthResponse(token, UserMapper.toResponse(user, buildings, activeBuildingId, gcsStorageService)));
        });

        javalin.post("/api/auth/confirm", ctx -> {
            ConfirmationRequest request = ctx.bodyValidator(ConfirmationRequest.class).get();
            userService.confirmUser(request.getToken());
            ctx.status(HttpStatus.OK);
            ctx.json(Map.of("message", "Usuario confirmado exitosamente"));
        });

        javalin.before("/api/users/*", authenticationHandler);
        javalin.before("/api/admin/*", authenticationHandler);
        javalin.before("/api/finance/*", authenticationHandler);
        javalin.before("/api/buildings/*", ctx -> {
            if ("/api/buildings/requests".equals(ctx.path())) {
                return;
            }
            authenticationHandler.handle(ctx);
        });
        javalin.before("/api/visits", authenticationHandler);
        javalin.before("/api/visits/*", authenticationHandler);
        javalin.before("/api/visit-contacts", authenticationHandler);
        javalin.before("/api/visit-contacts/*", authenticationHandler);
        javalin.before("/api/incidents", authenticationHandler);
        javalin.before("/api/incidents/*", authenticationHandler);
        javalin.before("/api/parcels", authenticationHandler);
        javalin.before("/api/parcels/*", authenticationHandler);
        javalin.before("/api/polls", authenticationHandler);
        javalin.before("/api/polls/*", authenticationHandler);
        javalin.before("/api/amenities", authenticationHandler);
        javalin.before("/api/amenities/*", authenticationHandler);
        javalin.before("/api/reservations", authenticationHandler);
        javalin.before("/api/reservations/*", authenticationHandler);
        javalin.before("/api/market", authenticationHandler);
        javalin.before("/api/market/*", authenticationHandler);
        javalin.before("/api/forum", authenticationHandler);
        javalin.before("/api/forum/*", authenticationHandler);
        javalin.before("/api/chat", authenticationHandler);
        javalin.before("/api/chat/*", authenticationHandler);
        javalin.before("/api/tasks", authenticationHandler);
        javalin.before("/api/tasks/*", authenticationHandler);
        javalin.before("/api/staff", authenticationHandler);
        javalin.before("/api/staff/*", authenticationHandler);
        javalin.before("/api/library", authenticationHandler);
        javalin.before("/api/library/*", authenticationHandler);

        // Extraer X-Building-Id header para filtrar datos por comunidad
        // La validación de acceso se hace en cada endpoint que use este valor
        javalin.before("/api/*", ctx -> {
            String buildingIdHeader = ctx.header("X-Building-Id");
            if (buildingIdHeader != null && !buildingIdHeader.isBlank()) {
                try {
                    Long selectedBuildingId = Long.parseLong(buildingIdHeader.trim());
                    ctx.attribute("selectedBuildingId", selectedBuildingId);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid X-Building-Id header value: {}", buildingIdHeader);
                }
            }
        });

        javalin.get("/api/users/me", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            var buildings = loadBuildings(user);
            Long activeBuildingId = resolveActiveBuildingId(user, buildings);
            UserResponse response = UserMapper.toResponseFromContext(ctx, buildings, activeBuildingId, gcsStorageService);
            ctx.json(response);
        });

        javalin.get("/api/users/me/unit", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null || user.unitId() == null) {
                ctx.status(HttpStatus.NO_CONTENT);
                return;
            }
            var unit = housingUnitRepository.findById(user.unitId()).orElse(null);
            if (unit == null) {
                ctx.status(HttpStatus.NO_CONTENT);
                return;
            }
            ctx.json(new UnitSummaryResponse(
                    unit.id(),
                    unit.buildingId(),
                    unit.number(),
                    unit.tower(),
                    unit.floor()));
        });

        javalin.put("/api/users/me/profile", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            UpdateProfileRequest request = ctx.bodyValidator(UpdateProfileRequest.class)
                    .check(r -> r.getFirstName() != null && !r.getFirstName().isBlank(), "firstName es requerido")
                    .check(r -> r.getLastName() != null && !r.getLastName().isBlank(), "lastName es requerido")
                    .check(r -> r.getPhone() != null && !r.getPhone().isBlank(), "phone es requerido")
                    .check(r -> r.getDocumentNumber() != null && !r.getDocumentNumber().isBlank(),
                            "documentNumber es requerido")
                    .get();
            User updated = userService.updateProfile(
                    user,
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhone(),
                    request.getDocumentNumber(),
                    request.getDisplayName());
            var buildings = loadBuildings(updated);
            Long activeBuildingId = resolveActiveBuildingId(updated, buildings);
            ctx.json(UserMapper.toResponse(updated, buildings, activeBuildingId, gcsStorageService));
        });

        javalin.post("/api/users/me/avatar", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            var file = ctx.uploadedFile("avatar");
            if (file == null) {
                throw new ValidationException("Archivo avatar es requerido");
            }
            userService.updateAvatar(user, file.filename(), file.content().readAllBytes());
            ctx.status(HttpStatus.NO_CONTENT);
        });

        javalin.post("/api/users/me/privacy-avatar", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            var file = ctx.uploadedFile("avatar");
            if (file == null) {
                throw new ValidationException("Archivo avatar es requerido");
            }
            userService.updatePrivacyAvatar(user, file.filename(), file.content().readAllBytes());
            ctx.status(HttpStatus.NO_CONTENT);
        });

        javalin.post("/api/users/me/password", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ChangePasswordRequest request = ctx.bodyValidator(ChangePasswordRequest.class)
                    .check(r -> r.getCurrentPassword() != null && !r.getCurrentPassword().isBlank(),
                            "currentPassword es requerido")
                    .check(r -> r.getNewPassword() != null && r.getNewPassword().length() >= 10,
                            "newPassword debe tener al menos 10 caracteres")
                    .get();
            userService.changePassword(user, request.getCurrentPassword(), request.getNewPassword());
            ctx.status(HttpStatus.NO_CONTENT);
        });

        javalin.post("/api/admin/users", ctx -> {
            User current = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (current == null || current.roleId() == null || current.roleId() != 1L) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(
                        ErrorResponse.of("Solo administradores pueden crear usuarios", HttpStatus.FORBIDDEN.getCode()));
                return;
            }
            RegistrationRequest request = validateRegistration(ctx.bodyValidator(RegistrationRequest.class));
            Long selectedBuildingId = validateSelectedBuilding(ctx, current);

            // Resolver unitNumber → unitId si se envió número de depto
            Long resolvedUnitId = request.getUnitId();
            if (resolvedUnitId == null && request.getUnitNumber() != null && selectedBuildingId != null) {
                String numberStr = String.valueOf(request.getUnitNumber());
                var unit = housingUnitRepository.findByBuildingIdAndNumber(selectedBuildingId, numberStr)
                        .orElseThrow(() -> new ValidationException(
                                "No existe la unidad " + request.getUnitNumber() + " en este edificio"));
                resolvedUnitId = unit.id();
            }

            String rawPassword = request.getPassword();
            if (rawPassword == null || rawPassword.isBlank()) {
                rawPassword = "1234567890";
            }

            User created = userService.adminCreateUser(
                    resolvedUnitId,
                    request.getRoleId(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getBirthDate(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getDocumentNumber(),
                    request.getResident(),
                    rawPassword,
                    selectedBuildingId);

            var buildings = loadBuildings(created);
            Long activeBuildingId = resolveActiveBuildingId(created, buildings);
            sendUserCredentialsEmail(created, rawPassword);
            ctx.status(HttpStatus.CREATED);
            ctx.json(UserMapper.toResponse(created, buildings, activeBuildingId, gcsStorageService));
        });

        // Obtener residentes del edificio seleccionado, agrupados por unidad
        javalin.get("/api/admin/residents", ctx -> {
            User current = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (current == null || current.roleId() == null ||
                    (current.roleId() != 1L && current.roleId() != 3L)) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(ErrorResponse.of("Solo administradores y conserjes pueden ver residentes",
                        HttpStatus.FORBIDDEN.getCode()));
                return;
            }
            Long selectedBuildingId = validateSelectedBuilding(ctx, current);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }
            var residents = userRepository.findResidentsByBuilding(selectedBuildingId);
            var response = residents.stream()
                    .map(com.domu.dto.ResidentResponse::from)
                    .toList();
            ctx.json(response);
        });

        // ===== HOUSING UNITS ENDPOINTS =====

        // Listar unidades del edificio seleccionado
        javalin.get("/api/admin/housing-units", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }
            var units = housingUnitService.listByBuilding(user.id(), selectedBuildingId);
            ctx.json(units);
        });

        // Obtener detalle de una unidad
        javalin.get("/api/admin/housing-units/{id}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long unitId = Long.parseLong(ctx.pathParam("id"));
            var unit = housingUnitService.getById(user.id(), unitId);
            ctx.json(unit);
        });

        // Crear nueva unidad
        javalin.post("/api/admin/housing-units", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }
            com.domu.dto.HousingUnitRequest request = ctx.bodyValidator(com.domu.dto.HousingUnitRequest.class)
                    .check(r -> r.getNumber() != null && !r.getNumber().isBlank(), "number es requerido")
                    .check(r -> r.getTower() != null && !r.getTower().isBlank(), "tower es requerida")
                    .check(r -> r.getFloor() != null && !r.getFloor().isBlank(), "floor es requerido")
                    .get();
            var created = housingUnitService.create(user.id(), selectedBuildingId, request);
            ctx.status(HttpStatus.CREATED);
            ctx.json(created);
        });

        // Actualizar unidad existente
        javalin.put("/api/admin/housing-units/{id}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long unitId = Long.parseLong(ctx.pathParam("id"));
            com.domu.dto.HousingUnitRequest request = ctx.bodyValidator(com.domu.dto.HousingUnitRequest.class)
                    .check(r -> r.getNumber() != null && !r.getNumber().isBlank(), "number es requerido")
                    .check(r -> r.getTower() != null && !r.getTower().isBlank(), "tower es requerida")
                    .check(r -> r.getFloor() != null && !r.getFloor().isBlank(), "floor es requerido")
                    .get();
            var updated = housingUnitService.update(user.id(), unitId, request);
            ctx.json(updated);
        });

        // Eliminar unidad (soft delete)
        javalin.delete("/api/admin/housing-units/{id}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long unitId = Long.parseLong(ctx.pathParam("id"));
            housingUnitService.delete(user.id(), unitId);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        // Vincular residente a unidad
        javalin.post("/api/admin/housing-units/{id}/residents", ctx -> {
            try {
                User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
                String unitIdParam = ctx.pathParam("id");
                Long unitId;
                try {
                    unitId = Long.parseLong(unitIdParam);
                } catch (NumberFormatException e) {
                    LOGGER.error("ID de unidad inválido: {}", unitIdParam);
                    ctx.status(HttpStatus.BAD_REQUEST);
                    ctx.json(ErrorResponse.of("ID de unidad inválido: " + unitIdParam,
                            HttpStatus.BAD_REQUEST.getCode()));
                    return;
                }

                com.domu.dto.LinkResidentToUnitRequest request = ctx
                        .bodyValidator(com.domu.dto.LinkResidentToUnitRequest.class)
                        .check(r -> r.getUserId() != null, "userId es requerido")
                        .get();

                LOGGER.info("Vinculando residente {} a unidad {}", request.getUserId(), unitId);
                housingUnitService.linkResident(user.id(), request.getUserId(), unitId);
                LOGGER.info("Residente {} vinculado exitosamente a unidad {}", request.getUserId(), unitId);
                ctx.status(HttpStatus.NO_CONTENT);
            } catch (com.domu.service.ValidationException e) {
                LOGGER.error("Error validando vinculación de residente", e);
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of(e.getMessage(), HttpStatus.BAD_REQUEST.getCode()));
            } catch (Exception e) {
                LOGGER.error("Error vinculando residente a unidad", e);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(ErrorResponse.of("Error al vincular residente: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getCode()));
            }
        });

        // Desvincular residente de unidad
        javalin.delete("/api/admin/housing-units/{id}/residents/{userId}", ctx -> {
            try {
                User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
                Long residentUserId = Long.parseLong(ctx.pathParam("userId"));
                housingUnitService.unlinkResident(user.id(), residentUserId);
                ctx.status(HttpStatus.NO_CONTENT);
            } catch (com.domu.service.ValidationException e) {
                LOGGER.error("Error validando desvinculación de residente", e);
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of(e.getMessage(), HttpStatus.BAD_REQUEST.getCode()));
            } catch (Exception e) {
                LOGGER.error("Error desvinculando residente de unidad", e);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.json(ErrorResponse.of("Error al desvincular residente: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getCode()));
            }
        });

        javalin.post("/api/finance/periods", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ensureAdmin(user);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            CreateCommonExpensePeriodRequest request = ctx.bodyValidator(CreateCommonExpensePeriodRequest.class).get();
            request.setBuildingId(selectedBuildingId);
            validateCreatePeriod(request);
            ctx.status(HttpStatus.CREATED);
            ctx.json(commonExpenseService.createPeriod(request, user, selectedBuildingId));
        });

        javalin.post("/api/finance/periods/{periodId}/charges", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ensureAdmin(user);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Long periodId = Long.parseLong(ctx.pathParam("periodId"));
            AddCommonChargesRequest request = validateAddCharges(ctx.bodyValidator(AddCommonChargesRequest.class));
            ctx.json(commonExpenseService.addCharges(periodId, request, user, selectedBuildingId));
        });

        javalin.get("/api/finance/periods", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ensureAdmin(user);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Integer fromIndex = parsePeriodIndex(ctx.queryParam("from"));
            Integer toIndex = parsePeriodIndex(ctx.queryParam("to"));
            ctx.json(commonExpenseService.listPeriodsForBuilding(selectedBuildingId, fromIndex, toIndex));
        });

        javalin.get("/api/finance/my-periods", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Integer fromIndex = parsePeriodIndex(ctx.queryParam("from"));
            Integer toIndex = parsePeriodIndex(ctx.queryParam("to"));
            ctx.json(commonExpenseService.listPeriodsForUser(user, selectedBuildingId, fromIndex, toIndex));
        });

        javalin.get("/api/finance/my-periods/{periodId}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Long periodId = Long.parseLong(ctx.pathParam("periodId"));
            var building = resolveBuildingSummary(user, selectedBuildingId);
            var unit = user != null && user.unitId() != null
                    ? housingUnitRepository.findById(user.unitId()).orElse(null)
                    : null;
            ctx.json(commonExpenseService.getPeriodDetailForUser(user, selectedBuildingId, periodId, building, unit));
        });

        javalin.get("/api/finance/my-periods/{periodId}/pdf", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Long periodId = Long.parseLong(ctx.pathParam("periodId"));
            var building = resolveBuildingSummary(user, selectedBuildingId);
            var unit = user != null && user.unitId() != null
                    ? housingUnitRepository.findById(user.unitId()).orElse(null)
                    : null;
            var detail = commonExpenseService.getPeriodDetailForUser(user, selectedBuildingId, periodId, building,
                    unit);
            byte[] pdf = commonExpensePdfService.buildResidentPeriodPdf(detail);
            String fileName = String.format("ggcc-%02d-%d.pdf", detail.month(), detail.year());
            ctx.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            ctx.contentType("application/pdf");
            ctx.result(pdf);
        });

        javalin.post("/api/finance/charges/{chargeId}/receipt", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ensureAdmin(user);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Long chargeId = Long.parseLong(ctx.pathParam("chargeId"));
            var document = extractReceiptDocument(ctx);
            var building = resolveBuildingSummary(user, selectedBuildingId);
            ctx.status(HttpStatus.CREATED);
            ctx.json(commonExpenseService.uploadChargeReceipt(chargeId, user, selectedBuildingId, building, document));
        });

        javalin.get("/api/finance/charges/{chargeId}/receipt", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = (user != null && user.roleId() != null && user.roleId() == 1L)
                    ? requireSelectedBuilding(ctx, user)
                    : validateSelectedBuilding(ctx, user);
            Long chargeId = Long.parseLong(ctx.pathParam("chargeId"));
            var receipt = commonExpenseService.downloadReceipt(chargeId, user, selectedBuildingId);
            String safeName = receipt.fileName() != null ? receipt.fileName() : "boleta.pdf";
            ctx.header("Content-Disposition", "attachment; filename=\"" + safeName + "\"");
            ctx.contentType("application/pdf");
            ctx.result(receipt.content());
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

        // RF_07: Pago simulado para demostración (con soporte de abono parcial)
        javalin.post("/api/finance/charges/{chargeId}/pay-simulated", ctx -> {
            Long chargeId = Long.parseLong(ctx.pathParam("chargeId"));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            SimulatedPaymentRequest request = ctx.bodyAsClass(SimulatedPaymentRequest.class);
            ctx.json(commonExpenseService.payChargeSimulated(
                    chargeId,
                    user,
                    request != null ? request.getAmount() : null,
                    request != null ? request.getPaymentMethod() : null
            ));
        });

        // Endpoint para descargar comprobante de pago
        javalin.get("/api/finance/payments/{paymentId}/receipt", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long paymentId = Long.parseLong(ctx.pathParam("paymentId"));
            var payment = commonExpenseService.getPaymentById(paymentId, user);

            // Get charge info for the receipt
            var chargeBalance = commonExpenseService.getChargeBalance(payment.chargeId());
            String chargeDescription = chargeBalance != null ? chargeBalance.charge().description() : "Gasto Común";

            // Get building and unit info
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            var building = resolveBuildingSummary(user, selectedBuildingId);
            var unit = user != null && user.unitId() != null
                    ? housingUnitRepository.findById(user.unitId()).orElse(null)
                    : null;
            String unitLabel = unit != null ? buildUnitLabel(unit) : "—";
            String buildingName = building != null ? building.name() : "—";

            byte[] pdf = paymentReceiptPdfService.buildPaymentReceipt(payment, buildingName, unitLabel, chargeDescription);
            String fileName = String.format("comprobante-pago-%d.pdf", paymentId);
            ctx.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            ctx.contentType("application/pdf");
            ctx.result(pdf);
        });

        // Endpoint para generar boleta de cargo
        javalin.get("/api/finance/charges/{chargeId}/boleta", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long chargeId = Long.parseLong(ctx.pathParam("chargeId"));
            var chargeContext = commonExpenseService.getChargeContext(chargeId, user);

            // Get building and unit info
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            var building = resolveBuildingSummary(user, selectedBuildingId);
            var unit = user != null && user.unitId() != null
                    ? housingUnitRepository.findById(user.unitId()).orElse(null)
                    : null;
            String unitLabel = unit != null ? buildUnitLabel(unit) : "—";
            String buildingName = building != null ? building.name() : "—";

            byte[] pdf = chargeReceiptPdfService.buildChargeReceipt(
                    chargeContext.charge(),
                    buildingName,
                    unitLabel,
                    chargeContext.year(),
                    chargeContext.month(),
                    chargeContext.dueDate()
            );
            String fileName = String.format("boleta-cargo-%d.pdf", chargeId);
            ctx.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            ctx.contentType("application/pdf");
            ctx.result(pdf);
        });

        javalin.post("/api/buildings/requests", ctx -> {
            CreateBuildingRequest request = parseCreateBuilding(ctx);
            CommunityRegistrationDocument document = extractRegistrationDocument(ctx);
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.status(HttpStatus.CREATED);
            BuildingRequestResponse response = buildingService.createRequest(request, user, document);
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
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            ctx.json(visitService.getVisitsForUser(user, selectedBuildingId));
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
            VisitFromContactRequest request = validateVisitFromContact(
                    ctx.bodyValidator(VisitFromContactRequest.class));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.status(HttpStatus.CREATED);
            ctx.json(visitContactService.registerFromContact(user, contactId, request));
        });

        javalin.post("/api/visits/qr-check", ctx -> {
            VisitorQrRequest request = ctx.bodyValidator(VisitorQrRequest.class)
                    .check(r -> r.getRun() != null && !r.getRun().isBlank(), "RUN es requerido")
                    .get();
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.json(visitService.processQrScan(user, request));
        });

        javalin.post("/api/visits/{authorizationId}/check-in", ctx -> {
            Long authorizationId = Long.parseLong(ctx.pathParam("authorizationId"));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.json(visitService.registerCheckIn(authorizationId, user));
        });

        javalin.get("/api/incidents/my", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            String buildingIdHeader = ctx.header("X-Building-Id");
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);

            LOGGER.info(
                    "GET /api/incidents/my - User: {}, Role: {}, Header X-Building-Id: {}, Validated BuildingId: {}",
                    user != null ? user.id() : "null",
                    user != null ? user.roleId() : "null",
                    buildingIdHeader,
                    selectedBuildingId);

            // Para administradores y conserjes, el buildingId es obligatorio
            if ((user.roleId() != null && (user.roleId() == 1L || user.roleId() == 3L)) && selectedBuildingId == null) {
                LOGGER.warn("Admin/Concierge intentó acceder a incidentes sin buildingId seleccionado. Header: {}",
                        buildingIdHeader);
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio para ver los incidentes",
                        HttpStatus.BAD_REQUEST.getCode()));
                return;
            }

            String from = ctx.queryParam("from");
            String to = ctx.queryParam("to");
            var result = incidentService.list(user, selectedBuildingId, parseDate(from), parseDate(to));
            LOGGER.info("Incidentes encontrados - Reported: {}, InProgress: {}, Closed: {}",
                    result.reported().size(), result.inProgress().size(), result.closed().size());
            ctx.json(result);
        });

        javalin.post("/api/incidents", ctx -> {
            IncidentRequest request = validateIncident(ctx.bodyValidator(IncidentRequest.class));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            ctx.status(HttpStatus.CREATED);
            ctx.json(incidentService.create(user, selectedBuildingId, request));
        });

        javalin.put("/api/incidents/{incidentId}/status", ctx -> {
            Long incidentId = Long.parseLong(ctx.pathParam("incidentId"));
            IncidentStatusUpdateRequest request = ctx.bodyValidator(IncidentStatusUpdateRequest.class)
                    .check(req -> req.getStatus() != null && !req.getStatus().isBlank(), "status es obligatorio")
                    .get();
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            ctx.json(incidentService.updateStatus(user, incidentId, selectedBuildingId, request.getStatus()));
        });

        javalin.patch("/api/incidents/{incidentId}/assign", ctx -> {
            Long incidentId = Long.parseLong(ctx.pathParam("incidentId"));
            IncidentAssignmentRequest request = ctx.bodyValidator(IncidentAssignmentRequest.class).get();
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            ctx.json(incidentService.updateAssignment(user, incidentId, selectedBuildingId, request.getAssignedToUserId()));
        });

        // ==================== PARCELS (ENCOMIENDAS) ====================

        javalin.get("/api/parcels/my", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            String status = ctx.queryParam("status");
            ctx.json(parcelService.listMyParcels(user, status));
        });

        javalin.get("/api/parcels", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }
            String status = ctx.queryParam("status");
            Long unitId = null;
            String unitIdParam = ctx.queryParam("unitId");
            if (unitIdParam != null && !unitIdParam.isBlank()) {
                try {
                    unitId = Long.parseLong(unitIdParam.trim());
                } catch (NumberFormatException e) {
                    throw new ValidationException("unitId debe ser numérico");
                }
            }
            ctx.json(parcelService.listForBuilding(user, selectedBuildingId, status, unitId));
        });

        javalin.post("/api/parcels", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            ParcelRequest request = ctx.bodyValidator(ParcelRequest.class).get();
            ctx.status(HttpStatus.CREATED);
            ctx.json(parcelService.create(user, selectedBuildingId, request));
        });

        javalin.put("/api/parcels/{parcelId}/status", ctx -> {
            Long parcelId = Long.parseLong(ctx.pathParam("parcelId"));
            ParcelStatusUpdateRequest request = ctx.bodyValidator(ParcelStatusUpdateRequest.class)
                    .check(req -> req.getStatus() != null && !req.getStatus().isBlank(), "status es obligatorio")
                    .get();
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            ctx.json(parcelService.updateStatus(user, parcelId, selectedBuildingId, request.getStatus()));
        });

        javalin.put("/api/parcels/{parcelId}", ctx -> {
            Long parcelId = Long.parseLong(ctx.pathParam("parcelId"));
            ParcelRequest request = ctx.bodyValidator(ParcelRequest.class).get();
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            ctx.json(parcelService.update(user, parcelId, selectedBuildingId, request));
        });

        javalin.delete("/api/parcels/{parcelId}", ctx -> {
            Long parcelId = Long.parseLong(ctx.pathParam("parcelId"));
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            parcelService.delete(user, parcelId, selectedBuildingId);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        javalin.get("/api/polls", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            String status = ctx.queryParam("status");
            ctx.json(pollService.list(user, status));
        });

        javalin.post("/api/polls", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            CreatePollRequest request = ctx.bodyValidator(CreatePollRequest.class).get();
            ctx.status(HttpStatus.CREATED);
            ctx.json(pollService.create(user, request));
        });

        javalin.get("/api/polls/{pollId}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long pollId = Long.parseLong(ctx.pathParam("pollId"));
            ctx.json(pollService.get(user, pollId));
        });

        javalin.post("/api/polls/{pollId}/votes", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long pollId = Long.parseLong(ctx.pathParam("pollId"));
            VoteRequest request = ctx.bodyValidator(VoteRequest.class).get();
            ctx.json(pollService.vote(user, pollId, request));
        });

        javalin.patch("/api/polls/{pollId}/close", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long pollId = Long.parseLong(ctx.pathParam("pollId"));
            ctx.json(pollService.close(user, pollId));
        });

        javalin.get("/api/polls/{pollId}/export", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long pollId = Long.parseLong(ctx.pathParam("pollId"));
            String csv = pollService.exportCsv(user, pollId);
            ctx.header("Content-Disposition", "attachment; filename=\"poll-" + pollId + ".csv\"");
            ctx.contentType("text/csv; charset=UTF-8");
            ctx.result(csv);
        });

        // ==================== AMENITIES (Áreas Comunes) ====================
        // IMPORTANTE: Las rutas específicas deben ir ANTES de las rutas con parámetros

        javalin.get("/api/amenities/all", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.json(amenityService.listAllAmenities(user));
        });

        javalin.get("/api/amenities", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.json(amenityService.listAmenities(user));
        });

        javalin.post("/api/amenities", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            AmenityRequest request = ctx.bodyValidator(AmenityRequest.class).get();
            ctx.status(HttpStatus.CREATED);
            ctx.json(amenityService.createAmenity(user, request));
        });

        javalin.get("/api/amenities/{amenityId}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long amenityId = Long.parseLong(ctx.pathParam("amenityId"));
            ctx.json(amenityService.getAmenity(user, amenityId));
        });

        javalin.put("/api/amenities/{amenityId}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long amenityId = Long.parseLong(ctx.pathParam("amenityId"));
            AmenityRequest request = ctx.bodyValidator(AmenityRequest.class).get();
            ctx.json(amenityService.updateAmenity(user, amenityId, request));
        });

        javalin.delete("/api/amenities/{amenityId}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long amenityId = Long.parseLong(ctx.pathParam("amenityId"));
            amenityService.deleteAmenity(user, amenityId);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        javalin.post("/api/amenities/{amenityId}/time-slots", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long amenityId = Long.parseLong(ctx.pathParam("amenityId"));
            TimeSlotRequest request = ctx.bodyValidator(TimeSlotRequest.class).get();
            ctx.json(amenityService.configureTimeSlots(user, amenityId, request));
        });

        javalin.get("/api/amenities/{amenityId}/availability", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long amenityId = Long.parseLong(ctx.pathParam("amenityId"));
            String date = ctx.queryParam("date");
            if (date == null || date.isBlank()) {
                date = java.time.LocalDate.now().toString();
            }
            ctx.json(amenityService.getAvailability(user, amenityId, date));
        });

        javalin.post("/api/amenities/{amenityId}/reserve", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long amenityId = Long.parseLong(ctx.pathParam("amenityId"));
            ReservationRequest request = ctx.bodyValidator(ReservationRequest.class).get();
            ctx.status(HttpStatus.CREATED);
            ctx.json(amenityService.createReservation(user, amenityId, request));
        });

        javalin.get("/api/amenities/{amenityId}/reservations", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long amenityId = Long.parseLong(ctx.pathParam("amenityId"));
            ctx.json(amenityService.getReservationsByAmenity(user, amenityId));
        });

        // ==================== RESERVATIONS ====================

        javalin.get("/api/reservations/my", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.json(amenityService.getMyReservations(user));
        });

        javalin.delete("/api/reservations/{reservationId}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long reservationId = Long.parseLong(ctx.pathParam("reservationId"));
            ctx.json(amenityService.cancelReservation(user, reservationId));
        });

        // ==================== MARKETPLACE ====================

        javalin.get("/api/market/items", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Long categoryId = ctx.queryParam("categoryId") != null ? Long.parseLong(ctx.queryParam("categoryId"))
                    : null;
            String status = ctx.queryParam("status");
            ctx.json(marketService.listItems(selectedBuildingId, categoryId, status));
        });

        javalin.get("/api/market/items/{id}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Long itemId = Long.parseLong(ctx.pathParam("id"));
            ctx.json(marketService.getItem(itemId, selectedBuildingId));
        });

        javalin.put("/api/market/items/{id}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long itemId = Long.parseLong(ctx.pathParam("id"));
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);

            MarketItemRequest request = new MarketItemRequest();
            request.setTitle(ctx.formParam("title"));
            request.setDescription(ctx.formParam("description"));
            request.setPrice(Double.parseDouble(ctx.formParam("price")));
            request.setCategoryId(Long.parseLong(ctx.formParam("categoryId")));

            // Procesar nuevas imágenes
            List<UploadedFile> files = ctx.uploadedFiles("images");
            java.util.List<com.domu.service.MarketService.ImageContent> imageList = new java.util.ArrayList<>();
            for (UploadedFile file : files) {
                imageList.add(new com.domu.service.MarketService.ImageContent(file.filename(),
                        file.content().readAllBytes()));
            }

            // Procesar URLs eliminadas
            String deletedJson = ctx.formParam("deletedImageUrls");
            java.util.List<String> deletedList = deletedJson != null
                    ? objectMapper.readValue(deletedJson, new TypeReference<List<String>>() {
                    })
                    : null;

            marketService.updateItem(itemId, user.id(), selectedBuildingId, request, imageList, deletedList);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        javalin.delete("/api/market/items/{id}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long itemId = Long.parseLong(ctx.pathParam("id"));
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            marketService.deleteItem(itemId, user.id(), selectedBuildingId);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        javalin.post("/api/market/items", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);

            MarketItemRequest request = new MarketItemRequest();
            request.setTitle(ctx.formParam("title"));
            request.setDescription(ctx.formParam("description"));
            request.setPrice(Double.parseDouble(ctx.formParam("price")));
            request.setCategoryId(Long.parseLong(ctx.formParam("categoryId")));
            request.setOriginalPriceLink(ctx.formParam("originalPriceLink"));

            List<UploadedFile> files = ctx.uploadedFiles("images");
            java.util.List<com.domu.service.MarketService.ImageContent> imageList = new java.util.ArrayList<>();

            for (UploadedFile file : files) {
                imageList.add(new com.domu.service.MarketService.ImageContent(
                        file.filename(),
                        file.content().readAllBytes()));
            }

            ctx.status(HttpStatus.CREATED);
            ctx.json(marketService.createItem(user.id(), selectedBuildingId, request, imageList));
        });

        // ==================== CHAT ====================

        javalin.get("/api/chat/rooms", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            ctx.json(chatService.getMyRooms(user.id(), selectedBuildingId));
        });

        javalin.post("/api/chat/rooms", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null) {
                throw new UnauthorizedResponse();
            }
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Long itemId = ctx.queryParam("itemId") != null ? Long.parseLong(ctx.queryParam("itemId")) : null;
            Long sellerId = Long.parseLong(ctx.queryParam("sellerId"));

            Long roomId = chatService.startConversation(selectedBuildingId, user.id(), sellerId, itemId);
            ctx.status(HttpStatus.CREATED);
            ctx.json(Map.of("roomId", roomId));
        });

        javalin.get("/api/chat/rooms/{roomId}/messages", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Long roomId = Long.parseLong(ctx.pathParam("roomId"));
            ctx.json(chatService.getMessages(roomId, user.id(), selectedBuildingId, 50));
        });

        javalin.delete("/api/chat/rooms/{roomId}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null) {
                throw new UnauthorizedResponse();
            }
            Long roomId = Long.parseLong(ctx.pathParam("roomId"));
            chatService.hideRoom(roomId, user.id());
            ctx.status(HttpStatus.NO_CONTENT);
        });

        javalin.get("/api/chat/online", ctx -> {
            ctx.json(chatWebSocketHandler.getOnlineUserIds());
        });

        javalin.get("/api/chat/neighbors", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null) {
                throw new UnauthorizedResponse();
            }
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            var neighbors = userRepository.findNeighborsForChat(selectedBuildingId, user.id());
            var resolved = neighbors.stream().map(n -> new com.domu.database.UserRepository.ChatNeighborSummary(
                    n.id(),
                    n.unitNumber(),
                    n.displayName(),
                    UserMapper.resolveUrl(n.avatarUrl(), gcsStorageService),
                    UserMapper.resolveUrl(n.privacyAvatarBoxId(), gcsStorageService)
            )).toList();
            ctx.json(resolved);
        });

        // ==================== CHAT REQUESTS & PROFILES ====================

        javalin.get("/api/users/{id}/profile", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null) {
                throw new UnauthorizedResponse();
            }
            Long targetUserId = Long.parseLong(ctx.pathParam("id"));
            Long buildingId = requireSelectedBuilding(ctx, user);
            ctx.json(userProfileService.getProfile(targetUserId, buildingId, user.id()));
        });

        javalin.get("/api/chat/requests/me", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            ctx.json(chatRequestService.getPendingRequests(user.id(), selectedBuildingId));
        });

        javalin.post("/api/chat/requests", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null) {
                throw new UnauthorizedResponse();
            }
            Long buildingId = requireSelectedBuilding(ctx, user);

            Map<String, Object> body = objectMapper.readValue(ctx.body(), new TypeReference<Map<String, Object>>() {
            });
            Long receiverId = Long.parseLong(body.get("receiverId").toString());
            Long itemId = body.get("itemId") != null ? Long.parseLong(body.get("itemId").toString()) : null;
            String message = (String) body.get("message");

            ctx.status(HttpStatus.CREATED);
            ctx.json(
                    Map.of("id", chatRequestService.createRequest(user.id(), receiverId, buildingId, itemId, message)));
        });

        javalin.put("/api/chat/requests/{id}/status", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Long requestId = Long.parseLong(ctx.pathParam("id"));
            Map<String, String> body = objectMapper.readValue(ctx.body(), new TypeReference<Map<String, String>>() {
            });
            chatRequestService.updateRequestStatus(requestId, body.get("status"), user.id(), selectedBuildingId);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        // ==================== FORUM ====================

        javalin.get("/api/forum/threads", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }
            ctx.json(forumService.getThreads(selectedBuildingId));
        });

        javalin.post("/api/forum/threads", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }
            CreateThreadRequest request = ctx.bodyValidator(CreateThreadRequest.class).get();
            forumService.createThread(selectedBuildingId, user, request);
            ctx.status(HttpStatus.CREATED);
        });

        javalin.put("/api/forum/threads/{threadId}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long threadId = Long.parseLong(ctx.pathParam("threadId"));
            CreateThreadRequest request = ctx.bodyValidator(CreateThreadRequest.class).get();
            forumService.updateThread(threadId, user, request);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        javalin.delete("/api/forum/threads/{threadId}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long threadId = Long.parseLong(ctx.pathParam("threadId"));
            forumService.deleteThread(threadId, user);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        // ==================== TASKS ====================

        javalin.get("/api/tasks", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            ctx.json(taskService.listByBuilding(user, selectedBuildingId));
        });

        javalin.post("/api/tasks", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            TaskRequest request = ctx.bodyValidator(TaskRequest.class).get();
            TaskRequest normalized = new TaskRequest(
                    selectedBuildingId,
                    request.title(),
                    request.description(),
                    request.assigneeId(),
                    request.assigneeIds(),
                    request.status(),
                    request.priority(),
                    request.dueDate(),
                    request.completedAt());
            ctx.status(HttpStatus.CREATED);
            ctx.json(taskService.create(user, selectedBuildingId, normalized));
        });

        javalin.put("/api/tasks/{id}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Long id = Long.parseLong(ctx.pathParam("id"));
            TaskRequest request = ctx.bodyValidator(TaskRequest.class).get();
            TaskRequest normalized = new TaskRequest(
                    selectedBuildingId,
                    request.title(),
                    request.description(),
                    request.assigneeId(),
                    request.assigneeIds(),
                    request.status(),
                    request.priority(),
                    request.dueDate(),
                    request.completedAt());
            ctx.json(taskService.update(user, selectedBuildingId, id, normalized));
        });

        javalin.delete("/api/tasks/{id}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = requireSelectedBuilding(ctx, user);
            Long id = Long.parseLong(ctx.pathParam("id"));
            taskService.delete(user, selectedBuildingId, id);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        // ==================== STAFF ====================

        javalin.get("/api/staff/me", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }

            var staff = staffService.findForUser(user, selectedBuildingId);
            if (staff.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(ErrorResponse.of("No se encontro perfil de personal para este usuario", HttpStatus.NOT_FOUND.getCode()));
                return;
            }

            ctx.json(staff.get());
        });

        javalin.get("/api/admin/staff", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null || user.roleId() == null || user.roleId() != 1L) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(ErrorResponse.of("Solo administradores pueden ver el personal", HttpStatus.FORBIDDEN.getCode()));
                return;
            }
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }
            ctx.json(staffService.listByBuilding(user, selectedBuildingId));
        });

        javalin.get("/api/admin/staff/active", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null || user.roleId() == null || user.roleId() != 1L) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(ErrorResponse.of("Solo administradores pueden ver el personal", HttpStatus.FORBIDDEN.getCode()));
                return;
            }
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }
            ctx.json(staffService.listActiveByBuilding(user, selectedBuildingId));
        });

        javalin.post("/api/admin/staff", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null || user.roleId() == null || user.roleId() != 1L) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(ErrorResponse.of("Solo administradores pueden crear personal", HttpStatus.FORBIDDEN.getCode()));
                return;
            }
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }
            StaffRequest request = ctx.bodyValidator(StaffRequest.class).get();
            // Asegurar que el buildingId del request coincida con el seleccionado
            StaffRequest validatedRequest = new StaffRequest(
                selectedBuildingId,
                request.firstName(),
                request.lastName(),
                request.rut(),
                request.email(),
                request.phone(),
                request.position(),
                request.active()
            );
            ctx.status(HttpStatus.CREATED);
            ctx.json(staffService.create(user, validatedRequest));
        });

        javalin.put("/api/admin/staff/{id}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null || user.roleId() == null || user.roleId() != 1L) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(ErrorResponse.of("Solo administradores pueden actualizar personal", HttpStatus.FORBIDDEN.getCode()));
                return;
            }
            Long id = Long.parseLong(ctx.pathParam("id"));
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }
            StaffRequest request = ctx.bodyValidator(StaffRequest.class).get();
            // Asegurar que el buildingId del request coincida con el seleccionado
            StaffRequest validatedRequest = new StaffRequest(
                selectedBuildingId,
                request.firstName(),
                request.lastName(),
                request.rut(),
                request.email(),
                request.phone(),
                request.position(),
                request.active()
            );
            ctx.json(staffService.update(user, id, validatedRequest));
        });

        javalin.delete("/api/admin/staff/{id}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null || user.roleId() == null || user.roleId() != 1L) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(ErrorResponse.of("Solo administradores pueden eliminar personal", HttpStatus.FORBIDDEN.getCode()));
                return;
            }
            Long id = Long.parseLong(ctx.pathParam("id"));
            staffService.delete(user, id);
            ctx.status(HttpStatus.NO_CONTENT);
        });

        // ==================== LIBRARY (BIBLIOTECA) ====================

        javalin.get("/api/library", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }
            ctx.json(libraryService.listDocuments(selectedBuildingId));
        });

        javalin.post("/api/library", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null || user.roleId() == null || user.roleId() != 1L) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(ErrorResponse.of("Solo administradores pueden subir documentos", HttpStatus.FORBIDDEN.getCode()));
                return;
            }
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            if (selectedBuildingId == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Debes seleccionar un edificio", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }

            String name = ctx.formParam("name");
            String category = ctx.formParam("category");
            var file = ctx.uploadedFile("file");

            if (name == null || name.isBlank() || category == null || category.isBlank() || file == null) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(ErrorResponse.of("Nombre, categoría y archivo son obligatorios", HttpStatus.BAD_REQUEST.getCode()));
                return;
            }

            try {
                byte[] content = file.content().readAllBytes();
                ctx.status(HttpStatus.CREATED);
                ctx.json(libraryService.uploadDocument(
                    selectedBuildingId,
                    user,
                    name,
                    category,
                    file.filename(),
                    content,
                    file.contentType()
                ));
            } catch (Exception e) {
                LOGGER.error("Error al procesar la subida del documento: {}", file.filename(), e);
                throw e; // El handler de Exception se encargará de devolver el 500
            }
        });

        javalin.delete("/api/library/{id}", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            if (user == null || user.roleId() == null || user.roleId() != 1L) {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(ErrorResponse.of("Solo administradores pueden eliminar documentos", HttpStatus.FORBIDDEN.getCode()));
                return;
            }
            Long selectedBuildingId = validateSelectedBuilding(ctx, user);
            Long docId = Long.parseLong(ctx.pathParam("id"));
            libraryService.deleteDocument(selectedBuildingId, docId, user);
            ctx.status(HttpStatus.NO_CONTENT);
        });
    }

    private String renderApprovalResultPage(boolean success, String title, String message) {
        String color = success ? "#1f8f5f" : "#c0392b";
        String border = success ? "rgba(31,143,95,0.18)" : "rgba(192,57,43,0.18)";
        String badge = success ? "Listo" : "No completado";
        String safeTitle = escapeHtml(title);
        String safeMessage = escapeHtml(message);
        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="es">
                        <head>
                          <meta charset="UTF-8">
                          <meta name="viewport" content="width=device-width, initial-scale=1.0">
                          <title>%s</title>
                          <style>
                            body { margin:0; padding:24px; background:#f4f6fb; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif; color:#111827; }
                            .card { max-width:520px; margin:0 auto; background:#ffffff; border-radius:14px; padding:28px 24px; box-shadow:0 8px 24px rgba(0,0,0,0.08); border:1px solid %s; }
                            .badge { display:inline-block; padding:6px 10px; border-radius:999px; font-size:12px; font-weight:700; color:#ffffff; background:%s; }
                            h1 { margin:16px 0 8px 0; font-size:22px; }
                            p { margin:0; line-height:1.6; color:#4b5563; }
                          </style>
                        </head>
                        <body>
                          <div class="card">
                            <span class="badge">%s</span>
                            <h1>%s</h1>
                            <p>%s</p>
                          </div>
                        </body>
                        </html>
                        """,
                safeTitle, border, color, badge, safeTitle, safeMessage);
    }

    private String renderRejectionForm(String code, String communityName) {
        String safeName = escapeHtml(communityName);
        String safeCode = escapeHtml(code);
        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="es">
                        <head>
                          <meta charset="UTF-8">
                          <meta name="viewport" content="width=device-width, initial-scale=1.0">
                          <title>Rechazar solicitud</title>
                          <style>
                            body { margin:0; padding:24px; background:#f4f6fb; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif; color:#111827; }
                            .card { max-width:580px; margin:0 auto; background:#ffffff; border-radius:14px; padding:28px 24px; box-shadow:0 8px 24px rgba(0,0,0,0.08); border:1px solid rgba(241,107,50,0.18); }
                            .badge { display:inline-block; padding:6px 10px; border-radius:999px; font-size:12px; font-weight:700; color:#ffffff; background:#f16b32; }
                            h1 { margin:16px 0 8px 0; font-size:22px; }
                            p { margin:0 0 14px 0; line-height:1.6; color:#4b5563; }
                            label { display:block; font-weight:600; margin-bottom:6px; color:#111827; }
                            textarea { width:100%%; min-height:130px; border:1px solid #e5e7eb; border-radius:10px; padding:12px; font-family:inherit; font-size:14px; resize:vertical; }
                            textarea:focus { outline:2px solid rgba(241,107,50,0.2); border-color:#f16b32; }
                            button { margin-top:14px; background:#f16b32; color:#ffffff; border:none; border-radius:12px; padding:12px 18px; font-weight:700; cursor:pointer; width:100%%; font-size:15px; }
                            button:hover { background:#dd5c28; }
                          </style>
                        </head>
                        <body>
                          <div class="card">
                            <span class="badge">Rechazar solicitud</span>
                            <h1>Ingresa el motivo</h1>
                            <p>Solicitud: %s</p>
                            <form method="post" action="/rechazar-solicitud">
                              <input type="hidden" name="code" value="%s">
                              <label for="reason">Motivo de rechazo</label>
                              <textarea id="reason" name="reason" maxlength="800" required placeholder="Explica por qué se rechaza el documento..."></textarea>
                              <button type="submit">Enviar rechazo</button>
                            </form>
                          </div>
                        </body>
                        </html>
                        """,
                safeName, safeCode);
    }

    private String renderAdminInviteForm(String code, AdminInviteInfoResponse invite) {
        String safeEmail = escapeHtml(invite.adminEmail());
        String safeCode = escapeHtml(code);
        String safeCommunity = escapeHtml(invite.communityName());
        String safeFirstName = escapeHtml(invite.firstName());
        String safeLastName = escapeHtml(invite.lastName());
        String safePhone = escapeHtml(invite.phone());
        String safeDocument = escapeHtml(invite.documentNumber());
        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="es">
                        <head>
                          <meta charset="UTF-8">
                          <meta name="viewport" content="width=device-width, initial-scale=1.0">
                          <title>Crear tu cuenta</title>
                          <style>
                            body { margin:0; padding:24px; background:#f4f6fb; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif; color:#111827; }
                            .card { max-width:620px; margin:0 auto; background:#ffffff; border-radius:14px; padding:28px 24px; box-shadow:0 8px 24px rgba(0,0,0,0.08); border:1px solid rgba(83,164,151,0.18); }
                            .badge { display:inline-block; padding:6px 10px; border-radius:999px; font-size:12px; font-weight:700; color:#ffffff; background:#53a497; }
                            h1 { margin:16px 0 8px 0; font-size:22px; }
                            p { margin:0 0 14px 0; line-height:1.6; color:#4b5563; }
                            label { display:block; font-weight:600; margin-bottom:6px; color:#111827; }
                            .field { margin-bottom:12px; }
                            input[type="text"], input[type="tel"], input[type="password"] { width:100%%; border:1px solid #e5e7eb; border-radius:10px; padding:12px; font-family:inherit; font-size:14px; }
                            input[type="text"]:focus, input[type="tel"]:focus, input[type="password"]:focus { outline:2px solid rgba(83,164,151,0.2); border-color:#53a497; }
                            button { margin-top:14px; background:#53a497; color:#ffffff; border:none; border-radius:12px; padding:12px 18px; font-weight:700; cursor:pointer; width:100%%; font-size:15px; }
                            button:hover { background:#3f897f; }
                            .readonly { padding:10px 12px; border-radius:10px; border:1px solid #e5e7eb; background:#f9fafb; }
                            .row { display:flex; gap:12px; }
                            .row .col { flex:1; }
                          </style>
                        </head>
                        <body>
                          <div class="card">
                            <span class="badge">Crear usuario administrador</span>
                            <h1>Para la comunidad %s</h1>
                            <p>Confirma y, si necesitas, ajusta tus datos antes de crear tu cuenta.</p>
                            <form method="post" action="/registrar-admin">
                              <input type="hidden" name="code" value="%s">
                              <div class="field">
                                <label>Correo</label>
                                <div class="readonly">%s</div>
                              </div>
                              <div class="row">
                                <div class="col field">
                                  <label for="firstName">Nombre</label>
                                  <input id="firstName" name="firstName" type="text" value="%s" required autocomplete="given-name" placeholder="Tu nombre">
                                </div>
                                <div class="col field">
                                  <label for="lastName">Apellido</label>
                                  <input id="lastName" name="lastName" type="text" value="%s" required autocomplete="family-name" placeholder="Tus apellidos">
                                </div>
                              </div>
                              <div class="field">
                                <label for="phone">Teléfono</label>
                                <input id="phone" name="phone" type="tel" value="%s" required autocomplete="tel" placeholder="+56 9 1234 5678">
                              </div>
                              <div class="field">
                                <label for="documentNumber">Documento</label>
                                <input id="documentNumber" name="documentNumber" type="text" value="%s" required autocomplete="off" placeholder="Rut / Documento">
                              </div>
                              <div class="field">
                                <label for="password">Contraseña (mínimo 10 caracteres)</label>
                                <input id="password" name="password" type="password" minlength="10" required autocomplete="new-password" placeholder="••••••••••">
                              </div>
                              <button type="submit">Crear mi cuenta</button>
                            </form>
                          </div>
                        </body>
                        </html>
                        """,
                safeCommunity, safeCode, safeEmail, safeFirstName, safeLastName, safePhone, safeDocument);
    }

    private String renderAdminInviteResult(boolean success, String title, String message) {
        String color = success ? "#1f8f5f" : "#c0392b";
        String border = success ? "rgba(31,143,95,0.18)" : "rgba(192,57,43,0.18)";
        String badge = success ? "Listo" : "No completado";
        String safeTitle = escapeHtml(title);
        String safeMessage = escapeHtml(message);
        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="es">
                        <head>
                          <meta charset="UTF-8">
                          <meta name="viewport" content="width=device-width, initial-scale=1.0">
                          <title>%s</title>
                          <style>
                            body { margin:0; padding:24px; background:#f4f6fb; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif; color:#111827; }
                            .card { max-width:520px; margin:0 auto; background:#ffffff; border-radius:14px; padding:28px 24px; box-shadow:0 8px 24px rgba(0,0,0,0.08); border:1px solid %s; }
                            .badge { display:inline-block; padding:6px 10px; border-radius:999px; font-size:12px; font-weight:700; color:#ffffff; background:%s; }
                            h1 { margin:16px 0 8px 0; font-size:22px; }
                            p { margin:0; line-height:1.6; color:#4b5563; }
                          </style>
                        </head>
                        <body>
                          <div class="card">
                            <span class="badge">%s</span>
                            <h1>%s</h1>
                            <p>%s</p>
                          </div>
                        </body>
                        </html>
                        """,
                safeTitle, border, color, badge, safeTitle, safeMessage);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
                .check(req -> req.getDocumentNumber() != null && !req.getDocumentNumber().isBlank(),
                        "documentNumber is required")
                .check(req -> req.getResident() != null, "resident is required")
                .check(req -> req.getEmail() != null && !req.getEmail().isBlank(), "email is required")
                .check(req -> req.getPassword() != null && req.getPassword().length() >= 10,
                        "password must contain at least 10 characters")
                .check(req -> !(req.getRoleId() != null && req.getRoleId() == 1L && req.getUnitId() == null),
                        "admin requiere unidad/edificio asignado")
                .get();
    }

    private LoginRequest validateLogin(BodyValidator<LoginRequest> validator) {
        return validator
                .check(req -> req.getEmail() != null && !req.getEmail().isBlank(), "email is required")
                .check(req -> req.getPassword() != null && !req.getPassword().isBlank(), "password is required")
                .get();
    }

    private CreateCommonExpensePeriodRequest validateCreatePeriod(CreateCommonExpensePeriodRequest request) {
        if (request == null) {
            throw new ValidationException("El cuerpo es obligatorio");
        }
        if (request.getBuildingId() == null) {
            throw new ValidationException("buildingId es requerido");
        }
        if (request.getYear() == null) {
            throw new ValidationException("year es requerido");
        }
        if (request.getMonth() == null) {
            throw new ValidationException("month es requerido");
        }
        if (request.getDueDate() == null) {
            throw new ValidationException("dueDate es requerido");
        }
        return request;
    }

    private AddCommonChargesRequest validateAddCharges(BodyValidator<AddCommonChargesRequest> validator) {
        return validator
                .check(req -> req.getCharges() != null && !req.getCharges().isEmpty(), "Debe existir al menos un cargo")
                .get();
    }

    private CommonPaymentRequest validatePayment(BodyValidator<CommonPaymentRequest> validator) {
        return validator
                .check(req -> req.getAmount() != null, "amount es requerido")
                .check(req -> req.getPaymentMethod() != null && !req.getPaymentMethod().isBlank(),
                        "paymentMethod es requerido")
                .get();
    }

    private CreateBuildingRequest parseCreateBuilding(Context ctx) {
        if (ctx.contentType() != null && ctx.contentType().toLowerCase().contains("multipart/form-data")) {
            CreateBuildingRequest request = new CreateBuildingRequest();
            request.setName(ctx.formParam("name"));
            request.setTowerLabel(ctx.formParam("towerLabel"));
            request.setAddress(ctx.formParam("address"));
            request.setCommune(ctx.formParam("commune"));
            request.setCity(ctx.formParam("city"));
            request.setAdminPhone(ctx.formParam("adminPhone"));
            request.setAdminEmail(ctx.formParam("adminEmail"));
            request.setAdminName(ctx.formParam("adminName"));
            request.setAdminDocument(ctx.formParam("adminDocument"));
            request.setFloors(parseInteger(ctx.formParam("floors"), "floors"));
            request.setUnitsCount(parseInteger(ctx.formParam("unitsCount"), "unitsCount"));
            request.setLatitude(parseDouble(ctx.formParam("latitude"), "latitude"));
            request.setLongitude(parseDouble(ctx.formParam("longitude"), "longitude"));
            request.setProofText(ctx.formParam("proofText"));
            return validateCreateBuilding(request);
        }
        return validateCreateBuilding(ctx.bodyValidator(CreateBuildingRequest.class).get());
    }

    private CreateBuildingRequest validateCreateBuilding(CreateBuildingRequest request) {
        if (request == null) {
            throw new ValidationException("El cuerpo es obligatorio");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException("name es requerido");
        }
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new ValidationException("address es requerido");
        }
        if (request.getCommune() == null || request.getCommune().isBlank()) {
            throw new ValidationException("commune es requerido");
        }
        if (request.getProofText() == null || request.getProofText().isBlank()) {
            throw new ValidationException("proofText es requerido");
        }
        if (request.getLatitude() != null && (request.getLatitude() < -90 || request.getLatitude() > 90)) {
            throw new ValidationException("latitude debe estar entre -90 y 90");
        }
        if (request.getLongitude() != null && (request.getLongitude() < -180 || request.getLongitude() > 180)) {
            throw new ValidationException("longitude debe estar entre -180 y 180");
        }
        return request;
    }

    private ApproveBuildingRequest validateApproveBuilding(BodyValidator<ApproveBuildingRequest> validator) {
        return validator.get();
    }

    private CreateVisitRequest validateCreateVisit(BodyValidator<CreateVisitRequest> validator) {
        return validator
                .check(req -> req.getVisitorName() != null && !req.getVisitorName().isBlank(),
                        "visitorName es requerido")
                .get();
    }

    private VisitContactRequest validateVisitContact(BodyValidator<VisitContactRequest> validator) {
        return validator
                .check(req -> req.getVisitorName() != null && !req.getVisitorName().isBlank(),
                        "visitorName es requerido")
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
        if (buildings != null && !buildings.isEmpty()) {
            if (user != null && user.unitId() != null) {
                Long buildingId = buildingRepository.findBuildingIdByUnitId(user.unitId());
                if (buildingId != null) {
                    boolean isInBuildings = buildings.stream().anyMatch(b -> b.id().equals(buildingId));
                    if (isInBuildings) {
                        return buildingId;
                    }
                }
            }
            return buildings.get(0).id();
        }
        if (user != null && user.unitId() != null) {
            return buildingRepository.findBuildingIdByUnitId(user.unitId());
        }
        return null;
    }

    /**
     * Valida que el usuario tenga acceso al edificio seleccionado.
     * Verifica acceso por:
     * 1. Tabla user_buildings (relación directa usuario-edificio)
     * 2. Unidad del usuario pertenece al edificio (relación
     * usuario-unidad-edificio)
     * Si tiene acceso, devuelve el buildingId; si no, devuelve null.
     */
    private Long validateSelectedBuilding(Context ctx, User user) {
        if (user == null) {
            LOGGER.warn("validateSelectedBuilding: user is null");
            return null;
        }
        Long selectedBuildingId = ctx.attribute("selectedBuildingId");
        String headerValue = ctx.header("X-Building-Id");

        if (selectedBuildingId == null && headerValue != null && !headerValue.isBlank()) {
            try {
                selectedBuildingId = Long.parseLong(headerValue.trim());
                ctx.attribute("selectedBuildingId", selectedBuildingId);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid X-Building-Id header value in validateSelectedBuilding: {}", headerValue);
            }
        }

        // Si no hay header, intentar resolver automáticamente
        if (selectedBuildingId == null) {
            // 1. Por unidad (Residentes)
            if (user.unitId() != null) {
                Long userBuildingId = buildingRepository.findBuildingIdByUnitId(user.unitId());
                if (userBuildingId != null) {
                    LOGGER.debug("Resolved building {} from unit {} for user {}", userBuildingId, user.unitId(),
                            user.id());
                    return userBuildingId;
                }
            }

            // 2. Si el usuario solo tiene acceso a UN edificio (Admin/Conserje único)
            var buildings = loadBuildings(user);
            if (buildings.size() == 1) {
                Long singleBuildingId = buildings.get(0).id();
                LOGGER.debug("Resolved single building {} for user {}", singleBuildingId, user.id());
                return singleBuildingId;
            }

            LOGGER.warn("validateSelectedBuilding: selectedBuildingId is null. Header X-Building-Id: {}, User: {}",
                    headerValue, user.id());
            return null;
        }

        // Verificar acceso directo por tabla user_buildings
        boolean hasAccess = userBuildingRepository.userHasAccessToBuilding(user.id(), selectedBuildingId);
        if (hasAccess) {
            LOGGER.debug("Usuario {} tiene acceso al edificio {} vía user_buildings", user.id(), selectedBuildingId);
            return selectedBuildingId;
        }

        // Verificar acceso indirecto por unidad del usuario
        if (user.unitId() != null) {
            Long userBuildingId = buildingRepository.findBuildingIdByUnitId(user.unitId());
            if (selectedBuildingId.equals(userBuildingId)) {
                LOGGER.debug("Usuario {} tiene acceso al edificio {} vía unidad {}", user.id(), selectedBuildingId,
                        user.unitId());
                return selectedBuildingId;
            }
        }

        LOGGER.warn("Usuario {} NO tiene acceso al edificio {}. Header X-Building-Id: {}",
                user.id(), selectedBuildingId, headerValue);
        return null;
    }

    private IncidentRequest validateIncident(BodyValidator<IncidentRequest> validator) {
        return validator
                .check(req -> req.getTitle() != null && !req.getTitle().isBlank(), "title es requerido")
                .check(req -> req.getDescription() != null && !req.getDescription().isBlank(),
                        "description es requerido")
                .check(req -> req.getCategory() != null && !req.getCategory().isBlank(), "category es requerido")
                .get();
    }

    private CommunityRegistrationDocument extractRegistrationDocument(Context ctx) {
        UploadedFile uploaded = ctx.uploadedFile("document");
        if (uploaded == null) {
            throw new ValidationException("document es requerido");
        }
        try (InputStream content = uploaded.content()) {
            byte[] bytes = content.readAllBytes();
            if (bytes.length == 0) {
                throw new ValidationException("El archivo de registro está vacío");
            }
            return new CommunityRegistrationDocument(
                    uploaded.filename() != null ? uploaded.filename() : "registro.pdf",
                    uploaded.contentType(),
                    bytes);
        } catch (IOException e) {
            throw new ValidationException("No se pudo leer el archivo de registro: " + e.getMessage());
        }
    }

    private com.domu.dto.CommonExpenseReceiptDocument extractReceiptDocument(Context ctx) {
        UploadedFile uploaded = ctx.uploadedFile("document");
        if (uploaded == null) {
            throw new ValidationException("document es requerido");
        }
        try (InputStream content = uploaded.content()) {
            byte[] bytes = content.readAllBytes();
            if (bytes.length == 0) {
                throw new ValidationException("El archivo de la boleta está vacío");
            }
            return new com.domu.dto.CommonExpenseReceiptDocument(
                    uploaded.filename() != null ? uploaded.filename() : "boleta.pdf",
                    uploaded.contentType(),
                    bytes);
        } catch (IOException e) {
            throw new ValidationException("No se pudo leer la boleta: " + e.getMessage());
        }
    }

    private Integer parseInteger(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " debe ser numérico");
        }
    }

    private Double parseDouble(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " debe ser numérico");
        }
    }

    private void ensureAdmin(User user) {
        if (user == null || user.roleId() == null || user.roleId() != 1L) {
            throw new UnauthorizedResponse("Solo administradores pueden realizar esta acción");
        }
    }

    private Long requireSelectedBuilding(Context ctx, User user) {
        Long selectedBuildingId = validateSelectedBuilding(ctx, user);
        if (selectedBuildingId == null) {
            throw new ValidationException("Debes seleccionar un edificio");
        }
        return selectedBuildingId;
    }

    private Integer parsePeriodIndex(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().split("-");
        if (parts.length != 2) {
            throw new ValidationException("Formato de período inválido. Usa YYYY-MM.");
        }
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12) {
                throw new ValidationException("Mes inválido en período.");
            }
            return year * 100 + month;
        } catch (NumberFormatException e) {
            throw new ValidationException("Período inválido. Usa YYYY-MM.");
        }
    }

    private BuildingSummaryResponse resolveBuildingSummary(User user, Long buildingId) {
        if (user == null || buildingId == null) {
            return null;
        }
        return loadBuildings(user).stream()
                .filter(b -> buildingId.equals(b.id()))
                .findFirst()
                .orElse(null);
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

    private String resolveFrontendBaseUrl() {
        String envValue = System.getenv("FRONTEND_BASE_URL");
        if (envValue != null && !envValue.isBlank()) {
            return envValue.replaceAll("/+$", "");
        }
        String sysProp = System.getProperty("frontend.base.url");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.replaceAll("/+$", "");
        }
        return "http://localhost:5173";
    }

    private void sendUserCredentialsEmail(User user, String rawPassword) {
        try {
            if (user == null || user.email() == null || user.email().isBlank()) {
                return;
            }

            String token = userService.getConfirmationToken(user.id());
            String frontendUrl = resolveFrontendBaseUrl();
            String confirmUrl = frontendUrl + "/confirmar?token=" + token;
            String loginUrl = frontendUrl + "/login";

            String subject = "Bienvenido a DOMU - Confirma tu cuenta";
            String roleLabel = "Usuario";
            if (user.roleId() != null && user.roleId() == 1L) {
                roleLabel = "Administrador";
            } else if (user.roleId() != null && user.roleId() == 2L) {
                roleLabel = "Residente";
            } else if (user.roleId() != null && user.roleId() == 3L) {
                roleLabel = "Conserje";
            } else if (user.roleId() != null && user.roleId() == 4L) {
                roleLabel = "Funcionario";
            }

            String html = """
                    <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                        <h2 style="color: #2c3e50;">¡Hola %s!</h2>
                        <p>Te damos la bienvenida a <strong>DOMU</strong>. Tu cuenta como <strong>%s</strong> ha sido registrada.</p>

                        <div style="background: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                            <p style="margin: 0;"><strong>Tus credenciales de acceso temporal:</strong></p>
                            <p style="margin: 10px 0 0;"><strong>Correo:</strong> %s</p>
                            <p style="margin: 5px 0 0;"><strong>Contraseña:</strong> %s</p>
                        </div>

                        <p>Por seguridad, debes confirmar tu cuenta en los próximos <strong>7 días</strong> haciendo clic en el siguiente botón:</p>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background: #1abc9c; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;">Confirmar mi cuenta</a>
                        </div>

                        <p style="font-size: 0.9em; color: #7f8c8d;">Si el botón no funciona, puedes copiar y pegar este enlace en tu navegador:<br/>
                        <a href="%s">%s</a></p>

                        <hr style="border: 0; border-top: 1px solid #eee; margin: 30px 0;" />
                        <p style="font-size: 0.8em; color: #95a5a6;">Una vez confirmada, podrás cambiar tu contraseña desde tu perfil en <a href="%s">DOMU</a>.</p>
                    </div>
                    """
                    .formatted(
                            user.firstName() != null ? user.firstName() : "usuario",
                            roleLabel,
                            user.email(),
                            rawPassword,
                            confirmUrl,
                            confirmUrl,
                            confirmUrl,
                            loginUrl);

            emailService.sendHtml(user.email(), subject, html);
        } catch (Exception e) {
            LOGGER.warn("No se pudo enviar el correo de credenciales: {}", e.getMessage());
        }
    }

    public Integer getPort() {
        return port;
    }

    private String buildUnitLabel(HousingUnit unit) {
        if (unit == null) {
            return null;
        }
        String number = unit.number() != null ? unit.number().trim() : "";
        String tower = unit.tower() != null && !unit.tower().isBlank() ? unit.tower().trim() : "";
        String floor = unit.floor() != null && !unit.floor().isBlank() ? unit.floor().trim() : "";
        StringBuilder label = new StringBuilder();
        if (!tower.isEmpty()) {
            label.append("Torre ").append(tower).append(" ");
        }
        label.append("Depto ").append(number);
        if (!floor.isEmpty()) {
            label.append(" - Piso ").append(floor);
        }
        return label.toString().trim();
    }
}
