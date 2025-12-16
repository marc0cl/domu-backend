package com.domu.dto;

public record BoxUploadResult(
        String requestFolderId,
        String statusFolderId,
        String fileId,
        String fileName
) {
}
