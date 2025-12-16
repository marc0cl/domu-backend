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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BuildingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildingService.class);
    private static final String INVALID_APPROVAL_LINK_MESSAGE = "El enlace no es válido o ya expiró.";

    private final BuildingRepository repository;
    private final CommunityRegistrationStorageService storageService;
    private final EmailService emailService;
    private final UserService userService;
    private final com.domu.config.AppConfig config;

    @Inject
    public BuildingService(BuildingRepository repository, CommunityRegistrationStorageService storageService, EmailService emailService, UserService userService, com.domu.config.AppConfig config) {
        this.repository = repository;
        this.storageService = storageService;
        this.emailService = emailService;
        this.userService = userService;
        this.config = config;
    }

    public BuildingRequestResponse createRequest(CreateBuildingRequest request, User user, CommunityRegistrationDocument document) {
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
                request.getFloors(),
                request.getUnitsCount(),
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
                null
        ));
        var uploadResult = storageService.uploadCommunityDocument(saved.id(), request.getName(), request.getCommune(), document);
        repository.updateBoxMetadata(saved.id(), uploadResult.requestFolderId(), uploadResult.fileId(), uploadResult.fileName());

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
                uploadResult.fileName()
        );
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
        repository.approveRequest(requestId, reviewer.id(), approveRequest != null ? approveRequest.getReviewNotes() : null, buildingId);
        if (request.boxFolderId() != null) {
            try {
                storageService.moveRequestToApproved(request.boxFolderId(), request.commune());
            } catch (Exception e) {
                LOGGER.warn("No se pudo mover la solicitud {} a 'aprobadas' en Box: {}", requestId, e.getMessage());
            }
        }

        String inviteUrl = buildAndPersistAdminInvite(request, buildingId);
        CompletableFuture.runAsync(() -> sendApprovedNotification(request, inviteUrl));

        return new BuildingRequestResponse(
                requestId,
                "APPROVED",
                buildingId,
                request.createdAt(),
                approveRequest != null ? approveRequest.getReviewNotes() : null,
                request.boxFolderId(),
                request.boxFileId(),
                request.boxFileName()
        );
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
        String inviteUrl = buildAndPersistAdminInvite(request, buildingId);
        CompletableFuture.runAsync(() -> sendApprovedNotification(request, inviteUrl));
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
    }

    private void sendApprovalPreview(CreateBuildingRequest request, Long requestId, CommunityRegistrationDocument document, String approvalCode) {
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
                    "rejectUrl", rejectUrl
            ));
            String to = (config.approvalsRecipient() != null && !config.approvalsRecipient().isBlank())
                    ? config.approvalsRecipient()
                    : request.getAdminEmail();
            String subject = "Solicitud de comunidad - " + request.getName();
            emailService.sendHtmlWithAttachment(to, subject, html, document.fileName(), document.contentType(), document.content());
            LOGGER.info("Correo de aprobación listo para solicitud {}. Destinatario: {}", requestId, to);
        } catch (Exception e) {
            LOGGER.warn("No se pudo generar el correo de aprobación para la solicitud {}: {}", requestId, e.getMessage());
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
                    "fileName", document.fileName()
            ));
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

    private record NameParts(String firstName, String lastName) { }

    private String buildAndPersistAdminInvite(BuildingRequest request, Long buildingId) {
        if (buildingId == null || request.adminEmail() == null || request.adminEmail().isBlank()) {
            return null;
        }
        String code = java.util.UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        repository.updateAdminInvite(request.id(), code, expiresAt);
        String baseUrl = config.approvalBaseUrl() != null && !config.approvalBaseUrl().isBlank()
                ? config.approvalBaseUrl()
                : "https://domu.app";
        return baseUrl + "/registrar-admin?code=" + code;
    }

    public BuildingRequest validateAdminInvite(String inviteCode) {
        return fetchValidAdminInvite(inviteCode);
    }

    public void registerAdminFromInvite(String inviteCode, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidationException("La contraseña es obligatoria");
        }
        BuildingRequest request = fetchValidAdminInvite(inviteCode);
        NameParts nameParts = splitName(request.adminName());
        userService.createAdminForBuilding(
                request.adminEmail(),
                request.adminPhone() != null ? request.adminPhone() : "",
                request.adminDocument() != null ? request.adminDocument() : "N/A",
                nameParts.firstName(),
                nameParts.lastName(),
                rawPassword,
                request.buildingId()
        );
        repository.markAdminInviteUsed(request.id());
    }

    private void sendApprovedNotification(BuildingRequest request, String adminInviteUrl) {
        try {
            if (request.adminEmail() == null || request.adminEmail().isBlank()) {
                LOGGER.warn("No se enviará notificación de aprobación: adminEmail vacío");
                return;
            }
            String location = buildLocation(request.commune(), request.city());
            String html = BuildingApprovedEmailTemplate.render(Map.of(
                    "communityName", request.name(),
                    "adminName", request.adminName(),
                    "location", location,
                    "fileName", request.boxFileName() != null ? request.boxFileName() : "documento.pdf",
                    "adminInviteUrl", adminInviteUrl != null ? adminInviteUrl : ""
            ));
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
                    "reason", escapeHtml(reason)
            ));
            String subject = "Solicitud rechazada - " + request.name();
            emailService.sendHtml(request.adminEmail(), subject, html);
            LOGGER.info("Correo de rechazo enviado a {}", request.adminEmail());
        } catch (Exception e) {
            LOGGER.warn("No se pudo enviar el correo de rechazo al solicitante: {}", e.getMessage());
        }
    }
}

