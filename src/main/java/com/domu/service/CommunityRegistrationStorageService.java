package com.domu.service;

import com.domu.dto.BoxUploadResult;
import com.domu.dto.CommunityRegistrationDocument;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles storage for community registration documents.
 * Delegates to GcsStorageService for actual cloud storage operations.
 */
@Singleton
public class CommunityRegistrationStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommunityRegistrationStorageService.class);
    private final GcsStorageService gcs;

    @Inject
    public CommunityRegistrationStorageService(GcsStorageService gcs) {
        this.gcs = gcs;
    }

    /**
     * Uploads a community registration document to GCS.
     *
     * @return BoxUploadResult with GCS references (folderId is the object path prefix)
     */
    public BoxUploadResult uploadCommunityDocument(Long requestId, String communityName, String commune,
            CommunityRegistrationDocument document) {
        if (requestId == null) {
            throw new ValidationException("requestId es requerido");
        }
        if (communityName == null || communityName.isBlank()) {
            throw new ValidationException("El nombre de la comunidad es requerido");
        }
        if (document == null || document.content() == null || document.content().length == 0) {
            throw new ValidationException("El documento de registro es obligatorio");
        }

        ImageOptimizer.validateDocument(document.content());

        String fileName = document.fileName() != null ? document.fileName() : "registro.pdf";
        String path = gcs.communityDocPath(requestId, fileName);
        String contentType = document.contentType() != null ? document.contentType() : "application/pdf";

        String url = gcs.upload(path, document.content(), contentType);
        LOGGER.info("Community document uploaded for request {}: {}", requestId, path);

        // Return with GCS path as folder/file references
        String folderPath = "community-docs/" + requestId;
        return new BoxUploadResult(folderPath, folderPath, path, fileName);
    }

    /**
     * Moves request to approved status — in GCS this is a no-op since we don't
     * use folder hierarchies for status tracking. Status is tracked in the DB.
     */
    public void moveRequestToApproved(String requestFolderId, String commune) {
        // No-op for GCS. Status is tracked in the database, not via folder moves.
        LOGGER.debug("moveRequestToApproved called (no-op for GCS): folder={}, commune={}", requestFolderId, commune);
    }
}
