package com.domu.web;

import com.domu.domain.BuildingRequest;
import com.domu.domain.core.User;
import com.domu.dto.ApproveBuildingRequest;
import com.domu.dto.AdminInviteInfoResponse;
import com.domu.dto.AdminInviteRegistrationRequest;
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
import com.domu.dto.CommunityRegistrationDocument;
import com.domu.dto.UpdateProfileRequest;
import com.domu.dto.ChangePasswordRequest;
import com.domu.dto.IncidentStatusUpdateRequest;
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
import com.domu.email.EmailService;

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
    private final EmailService emailService;
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
            final BuildingRepository buildingRepository,
            final EmailService emailService) {
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
        this.emailService = emailService;
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

        javalin.get("/api/users/me", ctx -> {
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            var buildings = loadBuildings(user);
            Long activeBuildingId = resolveActiveBuildingId(user, buildings);
            UserResponse response = UserMapper.toResponseFromContext(ctx, buildings, activeBuildingId);
            ctx.json(response);
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
                    request.getDocumentNumber());
            var buildings = loadBuildings(updated);
            Long activeBuildingId = resolveActiveBuildingId(updated, buildings);
            ctx.json(UserMapper.toResponse(updated, buildings, activeBuildingId));
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
            if (request.getRoleId() == null || (request.getRoleId() != 3L && request.getRoleId() != 4L)) {
                throw new ValidationException("Solo puedes crear conserjes (rol 3) o funcionarios (rol 4)");
            }
            // Forzar resident=false para estos roles
            request.setResident(false);
            String rawPassword = request.getPassword();
            if (rawPassword == null || rawPassword.isBlank()) {
                rawPassword = "1234567890";
            }

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
                    rawPassword);
            var buildings = loadBuildings(created);
            Long activeBuildingId = resolveActiveBuildingId(created, buildings);
            sendUserCredentialsEmail(created, rawPassword);
            ctx.status(HttpStatus.CREATED);
            ctx.json(UserMapper.toResponse(created, buildings, activeBuildingId));
        });

        javalin.post("/api/finance/periods", ctx -> {
            CreateCommonExpensePeriodRequest request = validateCreatePeriod(
                    ctx.bodyValidator(CreateCommonExpensePeriodRequest.class));
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
            VisitFromContactRequest request = validateVisitFromContact(
                    ctx.bodyValidator(VisitFromContactRequest.class));
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

        javalin.put("/api/incidents/{incidentId}/status", ctx -> {
            Long incidentId = Long.parseLong(ctx.pathParam("incidentId"));
            IncidentStatusUpdateRequest request = ctx.bodyValidator(IncidentStatusUpdateRequest.class)
                    .check(req -> req.getStatus() != null && !req.getStatus().isBlank(), "status es obligatorio")
                    .get();
            User user = ctx.attribute(AuthenticationHandler.USER_ATTRIBUTE);
            ctx.json(incidentService.updateStatus(user, incidentId, request.getStatus()));
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

    private CreateCommonExpensePeriodRequest validateCreatePeriod(
            BodyValidator<CreateCommonExpensePeriodRequest> validator) {
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
            String loginUrl = resolveFrontendBaseUrl() + "/login";
            String subject = "Tu acceso a DOMU";
            String roleLabel = "Funcionario";
            if (user.roleId() != null && user.roleId() == 3L) {
                roleLabel = "Conserje";
            } else if (user.roleId() != null && user.roleId() == 4L) {
                roleLabel = "Funcionario";
            }
            String html = """
                    <h2>Hola %s,</h2>
                    <p>Tu cuenta en DOMU fue creada.</p>
                    <p><strong>Correo:</strong> %s<br/>
                    <strong>Contraseña:</strong> %s<br/>
                    <strong>Rol:</strong> %s</p>
                    <p>Puedes iniciar sesión aquí: <a href="%s">%s</a></p>
                    """.formatted(
                    user.firstName() != null ? user.firstName() : "usuario",
                    user.email(),
                    rawPassword,
                    roleLabel,
                    loginUrl,
                    loginUrl);
            emailService.sendHtml(user.email(), subject, html);
        } catch (Exception e) {
            LOGGER.warn("No se pudo enviar el correo de credenciales: {}", e.getMessage());
        }
    }

    public Integer getPort() {
        return port;
    }
}
