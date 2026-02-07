package com.domu.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles storage for marketplace images, chat audio, and profile images.
 * Delegates to GcsStorageService for actual cloud storage operations.
 */
@Singleton
public class MarketplaceStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketplaceStorageService.class);
    private final GcsStorageService gcs;

    @Inject
    public MarketplaceStorageService(GcsStorageService gcs) {
        this.gcs = gcs;
    }

    /**
     * Uploads a marketplace item image, optimized for size.
     *
     * @return GCS object path — signed URL is generated at read time
     */
    public String uploadMarketImage(Long buildingId, Long itemId, String fileName, byte[] content) {
        byte[] optimized = ImageOptimizer.optimizeMarketImage(content);
        String ext = ImageOptimizer.outputExtension();
        String baseName = stripExtension(fileName) + "." + ext;
        String path = gcs.marketImagePath(buildingId, itemId, baseName);
        gcs.upload(path, optimized, ImageOptimizer.outputContentType());
        return path; // Store path, not URL
    }

    /**
     * Uploads a chat audio file (no optimization).
     *
     * @return public URL of the uploaded file
     */
    public String uploadChatAudio(Long buildingId, Long roomId, String fileName, byte[] content) {
        String path = gcs.chatAudioPath(roomId, fileName);
        String contentType = guessContentType(fileName);
        return gcs.upload(path, content, contentType);
    }

    /**
     * Uploads a profile image (avatar), optimized.
     *
     * @return GCS object path (not URL) — signed URL is generated at read time
     */
    public String uploadProfileImage(Long userId, String fileName, byte[] content) {
        byte[] optimized = ImageOptimizer.optimizeAvatar(content);
        String ext = ImageOptimizer.outputExtension();
        String path = gcs.profileAvatarPath(userId, ext);
        gcs.upload(path, optimized, ImageOptimizer.outputContentType());
        return path; // Store the object path, not the URL
    }

    /**
     * Uploads a privacy avatar image, optimized.
     *
     * @return GCS object path (not URL) — signed URL is generated at read time
     */
    public String uploadPrivacyImage(Long userId, String fileName, byte[] content) {
        byte[] optimized = ImageOptimizer.optimizeAvatar(content);
        String ext = ImageOptimizer.outputExtension();
        String path = gcs.profilePrivacyAvatarPath(userId, ext);
        gcs.upload(path, optimized, ImageOptimizer.outputContentType());
        return path; // Store the object path, not the URL
    }

    private String stripExtension(String fileName) {
        if (fileName == null) return "image";
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private String guessContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".webm")) return "audio/webm";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        return "application/octet-stream";
    }
}
