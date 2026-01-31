package com.domu.service;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxAPIResponseException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.domu.config.AppConfig;
import com.google.inject.Inject;
import com.domu.dto.CommonChargeReceiptUploadResult;
import com.domu.dto.CommonExpenseReceiptDocument;
import com.domu.dto.BuildingSummaryResponse;
import com.domu.domain.core.HousingUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

public class CommonExpenseReceiptStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExpenseReceiptStorageService.class);
    private static final String ROOT_FOLDER_NAME = "Gastos comunes";
    private static final String RECEIPTS_FOLDER = "Boletas";

    private final Supplier<BoxAPIConnection> connectionSupplier;
    private final String rootFolderId;

    @Inject
    public CommonExpenseReceiptStorageService(AppConfig config) {
        this(() -> {
            if (config.boxDeveloperToken() == null || config.boxDeveloperToken().isBlank()) {
                throw new ValidationException("BOX_TOKEN no configurado en el servidor");
            }
            return new BoxAPIConnection(config.boxDeveloperToken());
        }, config.boxRootFolderId() != null && !config.boxRootFolderId().isBlank() ? config.boxRootFolderId() : "0");
    }

    CommonExpenseReceiptStorageService(Supplier<BoxAPIConnection> connectionSupplier, String rootFolderId) {
        this.connectionSupplier = connectionSupplier;
        this.rootFolderId = rootFolderId;
    }

    public CommonChargeReceiptUploadResult uploadReceipt(
            BuildingSummaryResponse building,
            HousingUnit unit,
            Integer year,
            Integer month,
            Long chargeId,
            String chargeDescription,
            CommonExpenseReceiptDocument document
    ) {
        if (building == null) {
            throw new ValidationException("No se pudo resolver el edificio para guardar la boleta");
        }
        if (document == null || document.content() == null || document.content().length == 0) {
            throw new ValidationException("La boleta está vacía");
        }
        BoxAPIConnection api = connectionSupplier.get();
        try {
            BoxFolder root = new BoxFolder(api, rootFolderId);
            BoxFolder.Info rootFolder = ensureFolder(root, ROOT_FOLDER_NAME);

            String buildingLabel = sanitizeFolderName(buildBuildingLabel(building), "edificio");
            BoxFolder.Info buildingFolder = ensureFolder(rootFolder.getResource(), buildingLabel);

            String periodLabel = String.format("Periodo-%d-%02d", year, month);
            BoxFolder.Info periodFolder = ensureFolder(buildingFolder.getResource(), periodLabel);

            BoxFolder.Info receiptsFolder = ensureFolder(periodFolder.getResource(), RECEIPTS_FOLDER);
            String unitLabel = sanitizeFolderName(buildUnitLabel(unit), "general");
            BoxFolder.Info unitFolder = ensureFolder(receiptsFolder.getResource(), unitLabel);

            String fileName = buildFileName(chargeId, chargeDescription, document.fileName());
            BoxFile.Info uploaded = uploadOrVersionFile(unitFolder.getResource(), document, fileName);
            return new CommonChargeReceiptUploadResult(unitFolder.getID(), uploaded.getID(), uploaded.getName(),
                    document.contentType());
        } catch (BoxAPIResponseException e) {
            LOGGER.error("Box error subiendo boleta: code={}, body={}", e.getResponseCode(), e.getMessage(), e);
            if (e.getResponseCode() == 401) {
                throw new ValidationException("Box 401: token inválido/expirado (BOX_TOKEN).");
            }
            if (e.getResponseCode() == 403) {
                throw new ValidationException("Box 403: sin permisos para crear en folderId=" + rootFolderId);
            }
            throw e;
        }
    }

    public DownloadedReceipt downloadReceipt(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new ValidationException("No hay boleta registrada");
        }
        BoxAPIConnection api = connectionSupplier.get();
        try {
            BoxFile file = new BoxFile(api, fileId);
            BoxFile.Info info = file.getInfo("name", "size");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            file.download(output);
            return new DownloadedReceipt(info.getName(), output.toByteArray());
        } catch (BoxAPIResponseException e) {
            LOGGER.error("Box error descargando boleta: code={}, body={}", e.getResponseCode(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            throw new ValidationException("No se pudo descargar la boleta: " + e.getMessage());
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

    private BoxFile.Info uploadOrVersionFile(BoxFolder folder, CommonExpenseReceiptDocument document, String fileName) {
        try (InputStream content = new ByteArrayInputStream(document.content())) {
            return folder.uploadFile(content, fileName);
        } catch (BoxAPIException conflict) {
            if (conflict.getResponseCode() == 409) {
                BoxFile.Info existing = findFile(folder, fileName)
                        .orElseThrow(() -> new ValidationException("Conflicto de archivo en Box sin entrada existente"));
                try (InputStream content = new ByteArrayInputStream(document.content())) {
                    return existing.getResource().uploadNewVersion(content);
                } catch (Exception ex) {
                    throw new ValidationException("Error subiendo nueva versión de la boleta: " + ex.getMessage());
                }
            }
            throw conflict;
        } catch (Exception e) {
            throw new ValidationException("No se pudo subir la boleta a Box: " + e.getMessage());
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

    private String buildBuildingLabel(BuildingSummaryResponse building) {
        String base = building.name() != null ? building.name() : "edificio";
        String address = building.address() != null ? building.address() : "";
        String commune = building.commune() != null ? building.commune() : "";
        return base + " - " + address + " - " + commune;
    }

    private String buildUnitLabel(HousingUnit unit) {
        if (unit == null) {
            return "General";
        }
        String tower = unit.tower() != null && !unit.tower().isBlank() ? unit.tower().trim() : "";
        String number = unit.number() != null ? unit.number().trim() : "";
        if (!tower.isEmpty()) {
            return "Torre " + tower + " - Depto " + number;
        }
        return "Depto " + number;
    }

    private String buildFileName(Long chargeId, String description, String originalFileName) {
        String suffix = originalFileName != null ? originalFileName : "boleta.pdf";
        String cleanDesc = description != null ? description : "gasto";
        String base = "cargo-" + chargeId + "-" + cleanDesc;
        String sanitized = sanitizeFolderName(base, "boleta");
        String extension = "";
        int dot = suffix.lastIndexOf('.');
        if (dot > 0 && dot < suffix.length() - 1) {
            extension = suffix.substring(dot);
        }
        return sanitized + extension;
    }

    private String sanitizeFolderName(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String cleaned = value.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "-")
                .replaceAll("\\s{2,}", " ")
                .replaceAll("-{2,}", "-");
        if (cleaned.length() > 120) {
            cleaned = cleaned.substring(0, 120);
        }
        return cleaned;
    }

    public record DownloadedReceipt(String fileName, byte[] content) {
    }
}
