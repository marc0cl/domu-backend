package com.domu.dto;

public record BoxUploadResult(
        String communityFolderId,
        String registrationFolderId,
        String fileId,
        String fileName
) {
}
