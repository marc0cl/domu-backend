package com.domu.service;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.domu.config.AppConfig;
import com.domu.dto.CommunityRegistrationDocument;
import com.domu.dto.BoxUploadResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class CommunityRegistrationStorageService {

    private static final String BOX_ROOT_FOLDER = "0";

    private final Supplier<BoxAPIConnection> connectionSupplier;

    @Inject
    public CommunityRegistrationStorageService(AppConfig config) {
        this(() -> {
            if (config.boxDeveloperToken() == null || config.boxDeveloperToken().isBlank()) {
                throw new ValidationException("BOX_TOKEN no configurado en el servidor");
            }
            return new BoxAPIConnection(config.boxDeveloperToken());
        });
    }

    CommunityRegistrationStorageService(Supplier<BoxAPIConnection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    public BoxUploadResult uploadCommunityDocument(Long requestId, String communityName, CommunityRegistrationDocument document) {
        if (requestId == null) {
            throw new ValidationException("requestId es requerido");
        }
        if (communityName == null || communityName.isBlank()) {
            throw new ValidationException("El nombre de la comunidad es requerido");
        }
        if (document == null) {
            throw new ValidationException("El documento de registro es obligatorio");
        }

        BoxAPIConnection api = connectionSupplier.get();
        BoxFolder root = new BoxFolder(api, BOX_ROOT_FOLDER);

        BoxFolder.Info communityFolder = ensureFolder(root, "community-" + requestId);
        BoxFolder.Info registrationFolder = ensureFolder(communityFolder.getResource(), "registro");

        BoxFile.Info uploaded = uploadOrVersionFile(registrationFolder.getResource(), document);
        return new BoxUploadResult(communityFolder.getID(), registrationFolder.getID(), uploaded.getID(), uploaded.getName());
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
}
