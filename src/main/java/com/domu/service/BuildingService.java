package com.domu.service;

import com.domu.database.BuildingRepository;
import com.domu.domain.BuildingRequest;
import com.domu.domain.core.User;
import com.domu.dto.ApproveBuildingRequest;
import com.domu.dto.BuildingRequestResponse;
import com.domu.dto.CommunityRegistrationDocument;
import com.domu.dto.CreateBuildingRequest;
import com.google.inject.Inject;

import io.javalin.http.UnauthorizedResponse;

import java.time.LocalDateTime;

public class BuildingService {

    private final BuildingRepository repository;
    private final CommunityRegistrationStorageService storageService;

    @Inject
    public BuildingService(BuildingRepository repository, CommunityRegistrationStorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    public BuildingRequestResponse createRequest(CreateBuildingRequest request, User user, CommunityRegistrationDocument document) {
        validateCreate(request);
        if (user == null || user.id() == null) {
            throw new UnauthorizedResponse("Usuario no autenticado");
        }
        BuildingRequest saved = repository.insertRequest(new BuildingRequest(
                null,
                user.id(),
                request.getName().trim(),
                request.getTowerLabel(),
                request.getAddress().trim(),
                request.getCommune(),
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
                null
        ));
        var uploadResult = storageService.uploadCommunityDocument(saved.id(), request.getName(), document);
        repository.updateBoxMetadata(saved.id(), uploadResult.communityFolderId(), uploadResult.fileId(), uploadResult.fileName());

        return new BuildingRequestResponse(
                saved.id(),
                saved.status(),
                saved.buildingId(),
                saved.createdAt(),
                saved.reviewNotes(),
                uploadResult.communityFolderId(),
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
}

