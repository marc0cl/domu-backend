package com.domu.service;

import com.domu.dto.BoxUploadResult;
import com.domu.dto.CommunityRegistrationDocument;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Handles storage for community registration documents.
 * Delegates to GcsStorageService for actual cloud storage operations.
 */
@Singleton
public class CommunityRegistrationStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommunityRegistrationStorageService.class);
    private static final String LOCAL_STORAGE_ROOT = "var/local-storage/community-docs";
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
        String contentType = document.contentType() != null ? document.contentType() : "application/pdf";
        String folderPath = "community-docs/" + requestId;
        String safeFileName = sanitizeFileName(fileName);
        String gcsPath = gcs.communityDocPath(requestId, safeFileName);

        try {
            gcs.upload(gcsPath, document.content(), contentType);
            LOGGER.info("Community document uploaded to GCS for request {}: {}", requestId, gcsPath);
            return new BoxUploadResult(folderPath, folderPath, gcsPath, safeFileName);
        } catch (Exception gcsError) {
            LOGGER.error("GCS upload failed for request {}. Falling back to local storage. Cause: {}",
                    requestId, gcsError.getMessage(), gcsError);
            String fallbackFileId = writeLocalFallback(requestId, safeFileName, document.content());
            LOGGER.warn("Community document stored locally for request {} with fileId={}", requestId, fallbackFileId);
            return new BoxUploadResult(folderPath, folderPath, fallbackFileId, safeFileName);
        }
    }

    /**
     * Moves request to approved status — in GCS this is a no-op since we don't
     * use folder hierarchies for status tracking. Status is tracked in the DB.
     */
    public void moveRequestToApproved(String requestFolderId, String commune) {
        // No-op for GCS. Status is tracked in the database, not via folder moves.
        LOGGER.debug("moveRequestToApproved called (no-op for GCS): folder={}, commune={}", requestFolderId, commune);
    }

    private String sanitizeFileName(String fileName) {
        String raw = fileName != null ? fileName : "registro.pdf";
        String baseName = Paths.get(raw).getFileName().toString().trim();
        if (baseName.isEmpty()) {
            return "registro.pdf";
        }
        return baseName.replaceAll("[\\r\\n]", "_");
    }

    private String writeLocalFallback(Long requestId, String fileName, byte[] content) {
        String fallbackFileId = "local-community-docs/" + requestId + "/" + fileName;
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path targetDir = projectRoot.resolve(LOCAL_STORAGE_ROOT).resolve(String.valueOf(requestId));
        Path targetFile = targetDir.resolve(fileName);
        try {
            Files.createDirectories(targetDir);
            Files.write(targetFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            LOGGER.info("Local fallback file written at {}", targetFile.toAbsolutePath());
            return fallbackFileId;
        } catch (IOException e) {
            throw new ValidationException("No se pudo guardar el documento localmente tras falla de GCS");
        }
    }
}
