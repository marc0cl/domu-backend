package com.domu.service;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxAPIResponseException;
import com.domu.config.AppConfig;
import com.domu.dto.CommunityRegistrationDocument;
import com.domu.dto.BoxUploadResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class CommunityRegistrationStorageService {

    private final Supplier<BoxAPIConnection> connectionSupplier;
    private final String rootFolderId;
    private static final Logger LOGGER = LoggerFactory.getLogger(CommunityRegistrationStorageService.class);
    private static final String REQUESTS_ROOT_NAME = "Solicitudes";
    private static final String PENDING_FOLDER_NAME = "pendientes";
    private static final String REVIEWED_FOLDER_NAME = "revisadas";
    private static final String APPROVED_FOLDER_NAME = "aprobadas";
    private static final String REJECTED_FOLDER_NAME = "rechazadas";

    @Inject
    public CommunityRegistrationStorageService(AppConfig config) {
        this(() -> {
            if (config.boxDeveloperToken() == null || config.boxDeveloperToken().isBlank()) {
                throw new ValidationException("BOX_TOKEN no configurado en el servidor");
            }
            return new BoxAPIConnection(config.boxDeveloperToken());
        }, config.boxRootFolderId() != null && !config.boxRootFolderId().isBlank() ? config.boxRootFolderId() : "0");
    }

    CommunityRegistrationStorageService(Supplier<BoxAPIConnection> connectionSupplier, String rootFolderId) {
        this.connectionSupplier = connectionSupplier;
        this.rootFolderId = rootFolderId;
    }

    public BoxUploadResult uploadCommunityDocument(Long requestId, String communityName, String commune, CommunityRegistrationDocument document) {
        if (requestId == null) {
            throw new ValidationException("requestId es requerido");
        }
        if (communityName == null || communityName.isBlank()) {
            throw new ValidationException("El nombre de la comunidad es requerido");
        }
        if (commune == null || commune.isBlank()) {
            throw new ValidationException("La comuna es requerida");
        }
        if (document == null) {
            throw new ValidationException("El documento de registro es obligatorio");
        }

        BoxAPIConnection api = connectionSupplier.get();
        try {
            BoxFolder root = new BoxFolder(api, rootFolderId);
            BoxFolder.Info requestsRoot = ensureFolder(root, REQUESTS_ROOT_NAME);

            String safeCommune = sanitizeFolderName(commune, "comuna-sin-nombre");
            BoxFolder.Info communeFolder = ensureFolder(requestsRoot.getResource(), safeCommune);

            BoxFolder.Info pendingFolder = ensureFolder(communeFolder.getResource(), PENDING_FOLDER_NAME);
            ensureFolder(communeFolder.getResource(), REVIEWED_FOLDER_NAME);
            ensureFolder(communeFolder.getResource(), APPROVED_FOLDER_NAME);
            ensureFolder(communeFolder.getResource(), REJECTED_FOLDER_NAME);

            String requestFolderName = buildRequestFolderName(requestId, communityName);
            BoxFolder.Info requestFolder = ensureFolder(pendingFolder.getResource(), requestFolderName);

            BoxFile.Info uploaded = uploadOrVersionFile(requestFolder.getResource(), document);
            return new BoxUploadResult(requestFolder.getID(), pendingFolder.getID(), uploaded.getID(), uploaded.getName());
        } catch (BoxAPIResponseException e) {
            LOGGER.error("Box error. requestId={}, commune={}, rootFolderId={}, code={}, requestIdBox={}, body={}",
                    requestId, commune, rootFolderId, e.getResponseCode(), e.getResponseCode(), e.getMessage(), e);

            if (e.getResponseCode() == 401) {
                throw new ValidationException("Box 401: token inválido/expirado (BOX_TOKEN).");
            }
            if (e.getResponseCode() == 403) {
                throw new ValidationException("Box 403: sin permisos para crear dentro del folderId=" + rootFolderId +
                        ". Configura un BOX_ROOT_FOLDER_ID donde ese usuario tenga Editor/Owner.");
            }
            throw e;
        }
    }

    public void moveRequestToApproved(String requestFolderId, String commune) {
        moveRequestToStatus(requestFolderId, commune, APPROVED_FOLDER_NAME);
    }

    private void moveRequestToStatus(String requestFolderId, String commune, String targetStatusFolderName) {
        if (requestFolderId == null || requestFolderId.isBlank()) {
            throw new ValidationException("El identificador del folder de la solicitud en Box es obligatorio");
        }
        if (commune == null || commune.isBlank()) {
            throw new ValidationException("La comuna es requerida para mover la solicitud en Box");
        }
        BoxAPIConnection api = connectionSupplier.get();
        try {
            BoxFolder root = new BoxFolder(api, rootFolderId);
            BoxFolder.Info requestsRoot = ensureFolder(root, REQUESTS_ROOT_NAME);
            BoxFolder.Info communeFolder = ensureFolder(requestsRoot.getResource(), sanitizeFolderName(commune, "comuna-sin-nombre"));
            BoxFolder.Info targetFolder = ensureFolder(communeFolder.getResource(), targetStatusFolderName);

            BoxFolder requestFolder = new BoxFolder(api, requestFolderId);
            BoxFolder.Info requestInfo = requestFolder.getInfo("parent");
            String currentParentId = requestInfo.getParent() != null ? requestInfo.getParent().getID() : null;

            if (targetFolder.getID().equals(currentParentId)) {
                return; // ya está en la carpeta correcta
            }

            requestFolder.move(targetFolder.getResource());
        } catch (BoxAPIResponseException e) {
            LOGGER.error("Box error moviendo solicitud {} a estado {}: code={}, body={}",
                    requestFolderId, targetStatusFolderName, e.getResponseCode(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            throw new ValidationException("No se pudo mover la carpeta de la solicitud en Box: " + e.getMessage());
        }
    }

    private BoxFolder.Info ensureFolder(BoxFolder parent, String folderName) {
        try {
            return parent.createFolder(folderName);
        } catch (BoxAPIException e) {
            if (e.getResponseCode() == 409) {
                return findChildFolder(parent, folderName)
                        .orElseThrow(() -> new ValidationException("No se pudo reutilizar la carpeta existente en Box"));
            }
            throw e;
        }
    }

    private Optional<BoxFolder.Info> findChildFolder(BoxFolder parent, String folderName) {
        for (BoxItem.Info item : parent) {
            if (item instanceof BoxFolder.Info info && folderName.equalsIgnoreCase(info.getName())) {
                return Optional.of(info);
            }
        }
        return Optional.empty();
    }

    private BoxFile.Info uploadOrVersionFile(BoxFolder folder, CommunityRegistrationDocument document) {
        try (InputStream content = new ByteArrayInputStream(document.content())) {
            return folder.uploadFile(content, document.fileName());
        } catch (BoxAPIException conflict) {
            if (conflict.getResponseCode() == 409) {
                BoxFile.Info existing = findFile(folder, document.fileName())
                        .orElseThrow(() -> new ValidationException("Conflicto de archivo en Box sin entrada existente"));
                try (InputStream content = new ByteArrayInputStream(document.content())) {
                    return existing.getResource().uploadNewVersion(content);
                } catch (Exception ex) {
                    throw new ValidationException("Error subiendo nueva versión del documento: " + ex.getMessage());
                }
            }
            throw conflict;
        } catch (Exception e) {
            throw new ValidationException("No se pudo subir el documento a Box: " + e.getMessage());
        }
    }

    private Optional<BoxFile.Info> findFile(BoxFolder folder, String fileName) {
        for (BoxItem.Info item : folder) {
            if (item instanceof BoxFile.Info info && fileName.equalsIgnoreCase(info.getName())) {
                return Optional.of(info);
            }
        }
        return Optional.empty();
    }

    private String sanitizeFolderName(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String cleaned = value.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "-")
                .replaceAll("-{2,}", "-");
        if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 100);
        }
        return cleaned;
    }

    private String buildRequestFolderName(Long requestId, String communityName) {
        String baseName = communityName != null ? communityName.trim() : "comunidad";
        String cleaned = sanitizeFolderName(baseName, "comunidad");
        return "solicitud-" + requestId + "-" + cleaned;
    }
}
