package com.domu.dto;

public record CommonChargeReceiptUploadResult(
        String folderId,
        String fileId,
        String fileName,
        String mimeType
) {
}
