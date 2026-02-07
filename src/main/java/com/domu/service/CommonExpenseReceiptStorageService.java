package com.domu.service;

import com.domu.dto.CommonChargeReceiptUploadResult;
import com.domu.dto.CommonExpenseReceiptDocument;
import com.domu.dto.BuildingSummaryResponse;
import com.domu.domain.core.HousingUnit;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles storage for common expense receipts.
 * Delegates to GcsStorageService for actual cloud storage operations.
 */
public class CommonExpenseReceiptStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExpenseReceiptStorageService.class);
    private final GcsStorageService gcs;

    @Inject
    public CommonExpenseReceiptStorageService(GcsStorageService gcs) {
        this.gcs = gcs;
    }

    /**
     * Uploads a receipt document to GCS.
     */
    public CommonChargeReceiptUploadResult uploadReceipt(
            BuildingSummaryResponse building,
            HousingUnit unit,
            Integer year,
            Integer month,
            Long chargeId,
            String chargeDescription,
            CommonExpenseReceiptDocument document) {

        if (building == null) {
            throw new ValidationException("No se pudo resolver el edificio para guardar la boleta");
        }
        if (document == null || document.content() == null || document.content().length == 0) {
            throw new ValidationException("La boleta está vacía");
        }

        ImageOptimizer.validateDocument(document.content());

        Long unitId = unit != null ? unit.id() : 0L;
        String fileName = buildFileName(chargeId, chargeDescription, document.fileName());
        String path = gcs.receiptPath(building.id(), year, month, unitId, fileName);
        String contentType = document.contentType() != null ? document.contentType() : "application/pdf";

        gcs.upload(path, document.content(), contentType);
        LOGGER.info("Receipt uploaded: {}", path);

        String folderPath = String.format("receipts/%d/%d-%02d/%d", building.id(), year, month, unitId);
        return new CommonChargeReceiptUploadResult(folderPath, path, fileName, contentType);
    }

    /**
     * Downloads a receipt from GCS.
     */
    public DownloadedReceipt downloadReceipt(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new ValidationException("No hay boleta registrada");
        }
        byte[] content = gcs.download(fileId);
        if (content == null) {
            throw new ValidationException("No se encontró la boleta en el almacenamiento");
        }

        String fileName = fileId.contains("/") ? fileId.substring(fileId.lastIndexOf('/') + 1) : fileId;
        return new DownloadedReceipt(fileName, content);
    }

    private String buildFileName(Long chargeId, String description, String originalFileName) {
        String suffix = originalFileName != null ? originalFileName : "boleta.pdf";
        String cleanDesc = description != null ? description.replaceAll("[\\\\/:*?\"<>|]", "-") : "gasto";
        String base = "cargo-" + chargeId + "-" + cleanDesc;
        if (base.length() > 120) base = base.substring(0, 120);

        String extension = "";
        int dot = suffix.lastIndexOf('.');
        if (dot > 0 && dot < suffix.length() - 1) {
            extension = suffix.substring(dot);
        }
        return base + extension;
    }

    public record DownloadedReceipt(String fileName, byte[] content) {
    }
}
