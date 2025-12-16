package com.domu.dto;

public record CommunityRegistrationDocument(
        String fileName,
        String contentType,
        byte[] content
) {
    public CommunityRegistrationDocument {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName is required");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("content is required");
        }
    }
}
