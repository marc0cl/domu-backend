package com.domu.service;

import com.domu.database.BuildingRepository;
import com.domu.domain.BuildingRequest;
import com.domu.domain.core.User;
import com.domu.dto.ApproveBuildingRequest;
import com.domu.dto.BuildingRequestResponse;
import com.domu.dto.CommunityRegistrationDocument;
import com.domu.dto.CreateBuildingRequest;
import com.google.inject.Inject;
import com.domu.email.ApprovalEmailTemplate;
import com.domu.email.BuildingApprovedEmailTemplate;
import com.domu.email.BuildingRejectedEmailTemplate;
import com.domu.email.EmailService;
import com.domu.email.UserConfirmationEmailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.http.UnauthorizedResponse;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BuildingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildingService.class);
    private static final String BUILDING_TYPE_HOUSE = "HOUSE";
    private static final String BUILDING_TYPE_APARTMENT = "APARTMENT";
    private static final String BUILDING_TYPE_MIXED = "MIXED";
    private static final Long ADMIN_ROLE_ID = 1L;
    private static final String INVALID_APPROVAL_LINK_MESSAGE = "El enlace no es válido o ya expiró.";

    private final BuildingRepository repository;
    private final CommunityRegistrationStorageService storageService;
    private final EmailService emailService;
    private final UserService userService;
    private final com.domu.config.AppConfig config;

    @Inject
    public BuildingService(BuildingRepository repository, CommunityRegistrationStorageService storageService,
            EmailService emailService, UserService userService, com.domu.config.AppConfig config) {
        this.repository = repository;
        this.storageService = storageService;
        this.emailService = emailService;
        this.userService = userService;
        this.config = config;
    }

    public BuildingRequestResponse createRequest(CreateBuildingRequest request, User user,
            CommunityRegistrationDocument document) {
        validateCreate(request);
        Long requesterId = user != null ? user.id() : null;
        String approvalCode = java.util.UUID.randomUUID().toString();
        LocalDateTime approvalExpiresAt = LocalDateTime.now().plusDays(7);
        BuildingRequest saved = repository.insertRequest(new BuildingRequest(
                null,
                requesterId,
                request.getName().trim(),
                request.getTowerLabel(),
                request.getAddress().trim(),
                request.getCommune().trim(),
                request.getCity(),
                request.getAdminPhone(),
                request.getAdminEmail(),
                request.getAdminName(),
                request.getAdminDocument(),
                request.getBuildingType(),
                request.getFloors(),
                request.getUnitsCount(),
                request.getHouseUnitsCount(),
                request.getApartmentUnitsCount(),
                request.getLatitude(),
                request.getLongitude(),
                request.getProofText(),
                null,
                null,
                null,
                "PENDING",
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                approvalCode,
                approvalExpiresAt,
                null,
                null,
                null,
                null,
                null));
        var uploadResult = storageService.uploadCommunityDocument(saved.id(), request.getName(), request.getCommune(),
                document);
        repository.updateBoxMetadata(saved.id(), uploadResult.requestFolderId(), uploadResult.fileId(),
                uploadResult.fileName());

        CompletableFuture.runAsync(() -> sendApprovalPreview(request, saved.id(), document, approvalCode));
        CompletableFuture.runAsync(() -> sendUserConfirmation(request, document));

        return new BuildingRequestResponse(
                saved.id(),
                saved.status(),
                saved.buildingId(),
                saved.createdAt(),
                saved.reviewNotes(),
                uploadResult.requestFolderId(),
                uploadResult.fileId(),
                uploadResult.fileName());
    }

    public BuildingRequestResponse approve(Long requestId, ApproveBuildingRequest approveRequest, User reviewer) {
        if (reviewer == null || reviewer.roleId() == null || reviewer.roleId() != 1L) {
            throw new UnauthorizedResponse("Solo administradores pueden aprobar solicitudes");
        }
        BuildingRequest request = repository.findRequest(requestId)
                .orElseThrow(() -> new ValidationException("Solicitud no encontrada"));
        if (!"PENDING".equalsIgnoreCase(request.status())) {
            throw new ValidationException("La solicitud ya fue procesada");
        }

        Long buildingId = repository.insertBuildingFromRequest(request, request.requestedByUserId());
        repository.approveRequest(requestId, reviewer.id(),
                approveRequest != null ? approveRequest.getReviewNotes() : null, buildingId);
        if (request.boxFolderId() != null) {
            try {
                storageService.moveRequestToApproved(request.boxFolderId(), request.commune());
            } catch (Exception e) {
                LOGGER.warn("No se pudo mover la solicitud {} a 'aprobadas' en Box: {}", requestId, e.getMessage());
            }
        }

        AdminOnboardingResult onboarding = buildAndPersistAdminInvite(request, buildingId);
        CompletableFuture.runAsync(() -> sendApprovedNotification(request, onboarding));

        return new BuildingRequestResponse(
                requestId,
                "APPROVED",
                buildingId,
                request.createdAt(),
                approveRequest != null ? approveRequest.getReviewNotes() : null,
                request.boxFolderId(),
                request.boxFileId(),
                request.boxFileName());
    }

    public BuildingRequest validateApprovalLink(String approvalCode) {
        return fetchValidPendingRequest(approvalCode);
    }

    public BuildingRequest approveByCode(String approvalCode) {
        BuildingRequest request = fetchValidPendingRequest(approvalCode);
        Long buildingId = repository.insertBuildingFromRequest(request, request.requestedByUserId());
        repository.approveRequestByCode(request.id(), "Aprobado desde correo", buildingId);
        if (request.boxFolderId() != null) {
            try {
                storageService.moveRequestToApproved(request.boxFolderId(), request.commune());
            } catch (Exception e) {
                LOGGER.warn("No se pudo mover la solicitud {} a 'aprobadas' en Box: {}", request.id(), e.getMessage());
            }
        }
        AdminOnboardingResult onboarding = buildAndPersistAdminInvite(request, buildingId);
        CompletableFuture.runAsync(() -> sendApprovedNotification(request, onboarding));
        return request;
    }

    public BuildingRequest rejectByCode(String approvalCode, String reason) {
        BuildingRequest request = fetchValidPendingRequest(approvalCode);
        String normalizedReason = normalizeReason(reason);
        repository.rejectRequest(request.id(), normalizedReason);
        CompletableFuture.runAsync(() -> sendRejectedNotification(request, normalizedReason));
        return request;
    }

    private void validateCreate(CreateBuildingRequest request) {
        if (request == null) {
            throw new ValidationException("El cuerpo es obligatorio");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException("name es obligatorio");
        }
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new ValidationException("address es obligatorio");
        }
        if (request.getCommune() == null || request.getCommune().isBlank()) {
            throw new ValidationException("commune es obligatorio");
        }
        if (request.getProofText() == null || request.getProofText().isBlank()) {
            throw new ValidationException("proofText es obligatorio");
        }
        if (request.getLatitude() != null && (request.getLatitude() < -90 || request.getLatitude() > 90)) {
            throw new ValidationException("latitude debe estar entre -90 y 90");
        }
        if (request.getLongitude() != null && (request.getLongitude() < -180 || request.getLongitude() > 180)) {
            throw new ValidationException("longitude debe estar entre -180 y 180");
        }

        String buildingType = normalizeBuildingType(request.getBuildingType());
        request.setBuildingType(buildingType);

        Integer unitsCount = normalizePositiveInt(request.getUnitsCount());
        Integer floors = normalizePositiveInt(request.getFloors());
        Integer houseUnitsCount = normalizePositiveInt(request.getHouseUnitsCount());
        Integer apartmentUnitsCount = normalizePositiveInt(request.getApartmentUnitsCount());

        if (buildingType.equals(BUILDING_TYPE_HOUSE)) {
            houseUnitsCount = houseUnitsCount != null ? houseUnitsCount : unitsCount;
            if (houseUnitsCount == null) {
                throw new ValidationException("houseUnitsCount es obligatorio para comunidades de casas");
            }
            floors = null;
            apartmentUnitsCount = null;
            unitsCount = houseUnitsCount;
        } else if (buildingType.equals(BUILDING_TYPE_APARTMENT)) {
            apartmentUnitsCount = apartmentUnitsCount != null ? apartmentUnitsCount : unitsCount;
            if (apartmentUnitsCount == null) {
                throw new ValidationException("apartmentUnitsCount es obligatorio para comunidades de departamentos");
            }
            if (floors == null) {
                throw new ValidationException("floors es obligatorio para comunidades de departamentos");
            }
            houseUnitsCount = null;
            unitsCount = apartmentUnitsCount;
        } else {
            if (houseUnitsCount == null) {
                throw new ValidationException("houseUnitsCount es obligatorio cuando el tipo es ambos");
            }
            if (apartmentUnitsCount == null) {
                throw new ValidationException("apartmentUnitsCount es obligatorio cuando el tipo es ambos");
            }
            if (floors == null) {
                throw new ValidationException("floors es obligatorio cuando el tipo es ambos");
            }
            unitsCount = houseUnitsCount + apartmentUnitsCount;
        }

        request.setFloors(floors);
        request.setHouseUnitsCount(houseUnitsCount);
        request.setApartmentUnitsCount(apartmentUnitsCount);
        request.setUnitsCount(unitsCount);
    }

    private String normalizeBuildingType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return BUILDING_TYPE_HOUSE;
        }
        String normalized = rawType.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals(BUILDING_TYPE_HOUSE)
                && !normalized.equals(BUILDING_TYPE_APARTMENT)
                && !normalized.equals(BUILDING_TYPE_MIXED)) {
            throw new ValidationException("buildingType debe ser HOUSE, APARTMENT o MIXED");
        }
        return normalized;
    }

    private Integer normalizePositiveInt(Integer value) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new ValidationException("Los valores numericos deben ser mayores a cero");
        }
        return value;
    }

    private void sendApprovalPreview(CreateBuildingRequest request, Long requestId,
            CommunityRegistrationDocument document, String approvalCode) {
        try {
            String baseUrl = config.approvalBaseUrl() != null && !config.approvalBaseUrl().isBlank()
                    ? config.approvalBaseUrl()
                    : "https://domu.app";
            String approveUrl = baseUrl + "/aprobar-solicitud?code=" + approvalCode;
            String rejectUrl = baseUrl + "/rechazar-solicitud?code=" + approvalCode;
            String html = ApprovalEmailTemplate.render(java.util.Map.of(
                    "communityName", request.getName(),
                    "adminName", request.getAdminName(),
                    "adminEmail", request.getAdminEmail(),
                    "address", request.getAddress(),
                    "approvalCode", approvalCode,
                    "approveUrl", approveUrl,
                    "rejectUrl", rejectUrl));
            String to = (config.approvalsRecipient() != null && !config.approvalsRecipient().isBlank())
                    ? config.approvalsRecipient()
                    : request.getAdminEmail();
            String subject = "Solicitud de comunidad - " + request.getName();
            emailService.sendHtmlWithAttachment(to, subject, html, document.fileName(), document.contentType(),
                    document.content());
            LOGGER.info("Correo de aprobación listo para solicitud {}. Destinatario: {}", requestId, to);
        } catch (Exception e) {
            LOGGER.warn("No se pudo generar el correo de aprobación para la solicitud {}: {}", requestId,
                    e.getMessage());
        }
    }

    private void sendUserConfirmation(CreateBuildingRequest request, CommunityRegistrationDocument document) {
        try {
            if (request.getAdminEmail() == null || request.getAdminEmail().isBlank()) {
                LOGGER.warn("No se enviará confirmación: adminEmail vacío");
                return;
            }
            String location = buildLocation(request.getCommune(), request.getCity());
            String html = UserConfirmationEmailTemplate.render(Map.of(
                    "communityName", request.getName(),
                    "adminName", request.getAdminName(),
                    "location", location,
                    "fileName", document.fileName()));
            String subject = "Recibimos tu solicitud para " + request.getName();
            emailService.sendHtml(request.getAdminEmail(), subject, html);
            LOGGER.info("Correo de confirmación enviado a {}", request.getAdminEmail());
        } catch (Exception e) {
            LOGGER.warn("No se pudo enviar el correo de confirmación al solicitante: {}", e.getMessage());
        }
    }

    private BuildingRequest fetchValidPendingRequest(String approvalCode) {
        if (approvalCode == null || approvalCode.isBlank()) {
            throw new ValidationException(INVALID_APPROVAL_LINK_MESSAGE);
        }
        BuildingRequest request = repository.findRequestByApprovalCode(approvalCode)
                .orElseThrow(() -> new ValidationException(INVALID_APPROVAL_LINK_MESSAGE));
        if (!"PENDING".equalsIgnoreCase(request.status())
                || request.approvalCodeUsedAt() != null
                || request.approvalCodeExpiresAt() == null
                || request.approvalCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException(INVALID_APPROVAL_LINK_MESSAGE);
        }
        return request;
    }

    private BuildingRequest fetchValidAdminInvite(String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new ValidationException("El enlace de invitación no es válido o ya expiró.");
        }
        BuildingRequest request = repository.findRequestByAdminInviteCode(inviteCode)
                .orElseThrow(() -> new ValidationException("El enlace de invitación no es válido o ya expiró."));
        if (!"APPROVED".equalsIgnoreCase(request.status())
                || request.buildingId() == null
                || request.adminInviteUsedAt() != null
                || request.adminInviteExpiresAt() == null
                || request.adminInviteExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("El enlace de invitación no es válido o ya expiró.");
        }
        if (request.adminEmail() == null || request.adminEmail().isBlank()) {
            throw new ValidationException("No se encontró el correo del administrador para esta invitación.");
        }
        return request;
    }

    private String normalizeReason(String rawReason) {
        if (rawReason == null || rawReason.isBlank()) {
            throw new ValidationException("Debes ingresar un motivo de rechazo.");
        }
        String trimmed = rawReason.trim();
        int maxLength = 800;
        if (trimmed.length() > maxLength) {
            trimmed = trimmed.substring(0, maxLength);
        }
        return trimmed;
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

    private String buildLocation(String commune, String city) {
        if (commune == null || commune.isBlank()) {
            return city == null ? "" : city;
        }
        if (city != null && !city.isBlank()) {
            return commune + ", " + city;
        }
        return commune;
    }

    private NameParts splitName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return new NameParts("Administrador", "DOMU");
        }
        String[] parts = rawName.trim().split("\\s+");
        if (parts.length == 1) {
            return new NameParts(parts[0], "DOMU");
        }
        String first = parts[0];
        String last = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        return new NameParts(first, last);
    }

    private record NameParts(String firstName, String lastName) {
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private AdminOnboardingResult buildAndPersistAdminInvite(BuildingRequest request, Long buildingId) {
        if (buildingId == null || request.adminEmail() == null || request.adminEmail().isBlank()) {
            return new AdminOnboardingResult(null, false);
        }
        String baseUrl = config.approvalBaseUrl() != null && !config.approvalBaseUrl().isBlank()
                ? config.approvalBaseUrl()
                : "http://localhost:5173";

        var existingUser = userService.findByEmail(request.adminEmail()).orElse(null);
        if (existingUser != null && ADMIN_ROLE_ID.equals(existingUser.roleId())) {
            NameParts nameParts = splitName(request.adminName());
            userService.ensureAdminForBuilding(
                    request.adminEmail(),
                    request.adminPhone() != null ? request.adminPhone() : "000000000",
                    request.adminDocument() != null ? request.adminDocument() : "N/A",
                    nameParts.firstName(),
                    nameParts.lastName(),
                    "existing-admin-link",
                    buildingId);
            repository.markAdminInviteUsed(request.id());
            return new AdminOnboardingResult(baseUrl + "/login", true);
        }

        String code = java.util.UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        repository.updateAdminInvite(request.id(), code, expiresAt);
        return new AdminOnboardingResult(baseUrl + "/registrar-admin?code=" + code, false);
    }

    public BuildingRequest validateAdminInvite(String inviteCode) {
        return fetchValidAdminInvite(inviteCode);
    }

    public com.domu.dto.AdminInviteInfoResponse getAdminInviteInfo(String inviteCode) {
        BuildingRequest request = fetchValidAdminInvite(inviteCode);
        NameParts nameParts = splitName(request.adminName());
        boolean existingAdminAccount = userService.findByEmail(request.adminEmail())
                .map(user -> ADMIN_ROLE_ID.equals(user.roleId()))
                .orElse(false);
        return new com.domu.dto.AdminInviteInfoResponse(
                request.name(),
                request.adminEmail(),
                nameParts.firstName(),
                nameParts.lastName(),
                request.adminPhone(),
                request.adminDocument(),
                existingAdminAccount);
    }

    public void registerAdminFromInvite(String inviteCode, String rawPassword) {
        registerAdminFromInvite(inviteCode, null, null, null, null, rawPassword);
    }

    public void registerAdminFromInvite(
            String inviteCode,
            String firstName,
            String lastName,
            String phone,
            String documentNumber,
            String rawPassword) {
        BuildingRequest request = fetchValidAdminInvite(inviteCode);
        NameParts nameParts = splitName(request.adminName());

        var existingUser = userService.findByEmail(request.adminEmail()).orElse(null);
        if (existingUser != null) {
            if (!ADMIN_ROLE_ID.equals(existingUser.roleId())) {
                throw new ValidationException("El correo ya existe con otro rol; usa un correo de administrador.");
            }
            userService.ensureAdminForBuilding(
                    request.adminEmail(),
                    request.adminPhone() != null ? request.adminPhone() : "000000000",
                    request.adminDocument() != null ? request.adminDocument() : "N/A",
                    nameParts.firstName(),
                    nameParts.lastName(),
                    "existing-admin-link",
                    request.buildingId());
            repository.markAdminInviteUsed(request.id());
            return;
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidationException("La contraseña es obligatoria");
        }

        String resolvedFirstName = isBlank(firstName) ? nameParts.firstName() : firstName.trim();
        String resolvedLastName = isBlank(lastName) ? nameParts.lastName() : lastName.trim();
        String resolvedPhone = isBlank(phone) ? request.adminPhone() : phone.trim();
        String resolvedDocument = isBlank(documentNumber) ? request.adminDocument() : documentNumber.trim();

        if (isBlank(resolvedFirstName) || isBlank(resolvedLastName)) {
            throw new ValidationException("Debes ingresar nombre y apellido");
        }
        if (isBlank(resolvedPhone)) {
            throw new ValidationException("Debes ingresar un teléfono de contacto");
        }
        if (isBlank(resolvedDocument)) {
            throw new ValidationException("Debes ingresar un documento de identidad");
        }

        userService.ensureAdminForBuilding(
                request.adminEmail(),
                resolvedPhone,
                resolvedDocument,
                resolvedFirstName,
                resolvedLastName,
                rawPassword,
                request.buildingId());
        repository.markAdminInviteUsed(request.id());
    }

    private void sendApprovedNotification(BuildingRequest request, AdminOnboardingResult onboarding) {
        try {
            if (request.adminEmail() == null || request.adminEmail().isBlank()) {
                LOGGER.warn("No se enviará notificación de aprobación: adminEmail vacío");
                return;
            }
            String location = buildLocation(request.commune(), request.city());
            String actionUrl = onboarding != null && onboarding.actionUrl() != null ? onboarding.actionUrl() : "";
            boolean existingAdmin = onboarding != null && onboarding.existingAdminLinked();
            String ctaLabel = existingAdmin ? "Iniciar sesión en DOMU" : "Crear usuario administrador";
            String html = BuildingApprovedEmailTemplate.render(Map.of(
                    "communityName", request.name(),
                    "adminName", request.adminName(),
                    "location", location,
                    "fileName", request.boxFileName() != null ? request.boxFileName() : "documento.pdf",
                    "adminInviteUrl", actionUrl,
                    "adminCtaLabel", ctaLabel));
            String subject = "Solicitud aprobada - " + request.name();
            emailService.sendHtml(request.adminEmail(), subject, html);
            LOGGER.info("Correo de aprobación enviado a {}", request.adminEmail());
        } catch (Exception e) {
            LOGGER.warn("No se pudo enviar el correo de aprobación al solicitante: {}", e.getMessage());
        }
    }

    private void sendRejectedNotification(BuildingRequest request, String reason) {
        try {
            if (request.adminEmail() == null || request.adminEmail().isBlank()) {
                LOGGER.warn("No se enviará notificación de rechazo: adminEmail vacío");
                return;
            }
            String location = buildLocation(request.commune(), request.city());
            String html = BuildingRejectedEmailTemplate.render(Map.of(
                    "communityName", request.name(),
                    "adminName", request.adminName(),
                    "location", location,
                    "reason", escapeHtml(reason)));
            String subject = "Solicitud rechazada - " + request.name();
            emailService.sendHtml(request.adminEmail(), subject, html);
            LOGGER.info("Correo de rechazo enviado a {}", request.adminEmail());
        } catch (Exception e) {
            LOGGER.warn("No se pudo enviar el correo de rechazo al solicitante: {}", e.getMessage());
        }
    }

    private record AdminOnboardingResult(String actionUrl, boolean existingAdminLinked) {
    }
}


