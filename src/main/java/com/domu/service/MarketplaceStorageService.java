package com.domu.service;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxSharedLink;
import com.box.sdk.sharedlink.BoxSharedLinkRequest;
import com.domu.config.AppConfig;
import com.domu.service.ValidationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class MarketplaceStorageService {

    private final Supplier<BoxAPIConnection> connectionSupplier;
    private final String rootFolderId;
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketplaceStorageService.class);
    private static final String MARKET_ROOT = "Marketplace";
    private static final String CHAT_ROOT = "Chats";
    private static final String PROFILES_ROOT = "Profiles";

    @Inject
    public MarketplaceStorageService(AppConfig config) {
        this(() -> {
            if (config.boxDeveloperToken() == null || config.boxDeveloperToken().isBlank()) {
                throw new ValidationException("BOX_TOKEN no configurado en el servidor");
            }
            return new BoxAPIConnection(config.boxDeveloperToken());
        }, config.boxRootFolderId() != null && !config.boxRootFolderId().isBlank() ? config.boxRootFolderId() : "0");
    }

    MarketplaceStorageService(Supplier<BoxAPIConnection> connectionSupplier, String rootFolderId) {
        this.connectionSupplier = connectionSupplier;
        this.rootFolderId = rootFolderId;
    }

    public String uploadMarketImage(Long buildingId, Long itemId, String fileName, byte[] content) {
        BoxAPIConnection api = connectionSupplier.get();
        try {
            BoxFolder root = new BoxFolder(api, rootFolderId);
            BoxFolder.Info marketRootInfo = ensureFolder(root, MARKET_ROOT);
            
            BoxFolder marketRoot = new BoxFolder(api, marketRootInfo.getID());
            BoxFolder.Info buildingFolderInfo = ensureFolder(marketRoot, "building_" + buildingId);
            
            BoxFolder buildingFolder = new BoxFolder(api, buildingFolderInfo.getID());
            BoxFolder.Info itemFolderInfo = ensureFolder(buildingFolder, "item_" + itemId);

            BoxFolder itemFolder = new BoxFolder(api, itemFolderInfo.getID());
            try (InputStream is = new ByteArrayInputStream(content)) {
                BoxFile.Info uploaded = itemFolder.uploadFile(is, fileName);
                
                // Crear un Shared Link público con acceso total para visualización directa
                BoxFile file = new BoxFile(api, uploaded.getID());
                BoxSharedLinkRequest sharedLinkRequest = new BoxSharedLinkRequest()
                        .access(BoxSharedLink.Access.OPEN)
                        .permissions(true, true); // Permitir descarga y previsualización
                
                file.createSharedLink(sharedLinkRequest);
                
                // Obtener la información del archivo y asegurar que devolvemos la URL de descarga directa
                BoxFile.Info fileInfo = file.getInfo("shared_link", "download_url");
                String downloadUrl = fileInfo.getSharedLink().getDownloadURL();
                
                // Si por alguna razón downloadUrl es null, intentamos construirla o usar la estática estándar
                return (downloadUrl != null) ? downloadUrl : "https://app.box.com/shared/static/" + uploaded.getID();
            }
        } catch (com.box.sdk.BoxAPIResponseException e) {
            LOGGER.error("Error de API de Box: code={}, message={}", e.getResponseCode(), e.getMessage());
            if (e.getResponseCode() == 401) {
                throw new ValidationException("El token de Box ha expirado. Por favor, actualiza el BOX_TOKEN en el archivo .env.");
            }
            if (e.getResponseCode() == 403) {
                throw new ValidationException("Sin permisos para subir archivos a la carpeta configurada en Box.");
            }
            throw new ValidationException("Error en la comunicación con Box: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error inesperado subiendo imagen a Box", e);
            throw new ValidationException("No se pudo subir la imagen a Box: " + e.getMessage());
        }
    }

    public String uploadChatAudio(Long buildingId, Long roomId, String fileName, byte[] content) {
        BoxAPIConnection api = connectionSupplier.get();
        try {
            BoxFolder root = new BoxFolder(api, rootFolderId);
            BoxFolder.Info chatRootInfo = ensureFolder(root, CHAT_ROOT);
            
            BoxFolder chatRoot = new BoxFolder(api, chatRootInfo.getID());
            BoxFolder.Info buildingFolderInfo = ensureFolder(chatRoot, "building_" + buildingId);
            
            BoxFolder buildingFolder = new BoxFolder(api, buildingFolderInfo.getID());
            BoxFolder.Info roomFolderInfo = ensureFolder(buildingFolder, "room_" + roomId);

            BoxFolder roomFolder = new BoxFolder(api, roomFolderInfo.getID());
            try (InputStream is = new ByteArrayInputStream(content)) {
                BoxFile.Info uploaded = roomFolder.uploadFile(is, fileName);
                return uploaded.getID();
            }
        } catch (Exception e) {
            LOGGER.error("Error subiendo audio a Box", e);
            throw new ValidationException("No se pudo subir the audio a Box: " + e.getMessage());
        }
    }

    public String uploadProfileImage(Long userId, String fileName, byte[] content) {
        BoxAPIConnection api = connectionSupplier.get();
        try {
            BoxFolder root = new BoxFolder(api, rootFolderId);
            BoxFolder.Info profilesRootInfo = ensureFolder(root, PROFILES_ROOT);
            
            BoxFolder profilesRoot = new BoxFolder(api, profilesRootInfo.getID());
            BoxFolder.Info userFolderInfo = ensureFolder(profilesRoot, "user_" + userId);

            BoxFolder userFolder = new BoxFolder(api, userFolderInfo.getID());
            try (InputStream is = new ByteArrayInputStream(content)) {
                BoxFile.Info uploaded = userFolder.uploadFile(is, fileName);
                
                BoxFile file = new BoxFile(api, uploaded.getID());
                BoxSharedLinkRequest sharedLinkRequest = new BoxSharedLinkRequest()
                        .access(BoxSharedLink.Access.OPEN)
                        .permissions(true, false);
                
                file.createSharedLink(sharedLinkRequest);
                BoxFile.Info fileInfo = file.getInfo("shared_link");
                return fileInfo.getSharedLink().getDownloadURL();
            }
        } catch (Exception e) {
            LOGGER.error("Error subiendo imagen de perfil a Box", e);
            throw new ValidationException("No se pudo subir la imagen de perfil a Box: " + e.getMessage());
        }
    }

    private BoxFolder.Info ensureFolder(BoxFolder parent, String folderName) {
        return findChildFolder(parent, folderName)
                .orElseGet(() -> {
                    try {
                        return parent.createFolder(folderName);
                    } catch (BoxAPIException e) {
                        return findChildFolder(parent, folderName)
                                .orElseThrow(() -> new ValidationException("Error creando/reutilizando carpeta en Box: " + folderName));
                    }
                });
    }

    private Optional<BoxFolder.Info> findChildFolder(BoxFolder parent, String folderName) {
        for (BoxItem.Info item : parent) {
            if (item instanceof BoxFolder.Info info && folderName.equalsIgnoreCase(info.getName())) {
                return Optional.of(info);
            }
        }
        return Optional.empty();
    }
}
