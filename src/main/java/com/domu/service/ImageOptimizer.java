package com.domu.service;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility for optimizing images before upload to cloud storage.
 * Enforces strict size limits to control GCS costs.
 */
public final class ImageOptimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageOptimizer.class);

    /** Maximum raw upload size before any processing (50 MB). */
    public static final long MAX_RAW_UPLOAD_BYTES = 50L * 1024 * 1024;

    /** Max output size for avatars: 500 KB. */
    private static final long MAX_AVATAR_BYTES = 500L * 1024;
    /** Max output size for marketplace images: 1 MB. */
    private static final long MAX_MARKET_BYTES = 1L * 1024 * 1024;
    /** Max output size for documents (PDF, etc.): 5 MB. */
    public static final long MAX_DOCUMENT_BYTES = 5L * 1024 * 1024;

    private ImageOptimizer() {
    }

    /**
     * Optimizes a profile avatar image.
     * Resizes to max 200x200, JPEG quality 80%.
     *
     * @param raw original image bytes
     * @return optimized JPEG bytes
     */
    public static byte[] optimizeAvatar(byte[] raw) {
        validateRawSize(raw);
        try {
            byte[] result = resize(raw, 200, 200, 0.80);
            if (result.length > MAX_AVATAR_BYTES) {
                // Try a lower quality pass
                result = resize(raw, 200, 200, 0.55);
            }
            LOGGER.debug("Avatar optimized: {} -> {} bytes ({}% reduction)",
                    raw.length, result.length, 100 - (result.length * 100 / raw.length));
            return result;
        } catch (IOException e) {
            LOGGER.warn("Could not optimize avatar, returning raw", e);
            return raw;
        }
    }

    /**
     * Optimizes a marketplace item image.
     * Resizes to max 1024x1024, JPEG quality 75%.
     *
     * @param raw original image bytes
     * @return optimized JPEG bytes
     */
    public static byte[] optimizeMarketImage(byte[] raw) {
        validateRawSize(raw);
        try {
            byte[] result = resize(raw, 1024, 1024, 0.75);
            if (result.length > MAX_MARKET_BYTES) {
                result = resize(raw, 800, 800, 0.60);
            }
            LOGGER.debug("Market image optimized: {} -> {} bytes ({}% reduction)",
                    raw.length, result.length, 100 - (result.length * 100 / raw.length));
            return result;
        } catch (IOException e) {
            LOGGER.warn("Could not optimize market image, returning raw", e);
            return raw;
        }
    }

    /**
     * Validates a document (PDF, etc.) against the max size.
     * No image transformation is performed.
     *
     * @param raw document bytes
     */
    public static void validateDocument(byte[] raw) {
        if (raw == null || raw.length == 0) {
            throw new ValidationException("El archivo está vacío.");
        }
        if (raw.length > MAX_DOCUMENT_BYTES) {
            throw new ValidationException(
                    String.format("El documento excede el límite de %d MB.", MAX_DOCUMENT_BYTES / (1024 * 1024)));
        }
    }

    /**
     * Returns "jpg" — the standard output format after optimization.
     */
    public static String outputExtension() {
        return "jpg";
    }

    /**
     * Returns the MIME type for optimized images.
     */
    public static String outputContentType() {
        return "image/jpeg";
    }

    // ─── internals ──────────────────────────────────────────────────────

    private static void validateRawSize(byte[] raw) {
        if (raw == null || raw.length == 0) {
            throw new ValidationException("El archivo de imagen está vacío.");
        }
        if (raw.length > MAX_RAW_UPLOAD_BYTES) {
            throw new ValidationException(
                    String.format("La imagen excede el límite de %d MB.", MAX_RAW_UPLOAD_BYTES / (1024 * 1024)));
        }
    }

    private static byte[] resize(byte[] raw, int maxWidth, int maxHeight, double quality) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(raw))
                .size(maxWidth, maxHeight)
                .keepAspectRatio(true)
                .outputFormat("jpg")
                .outputQuality(quality)
                .toOutputStream(out);
        return out.toByteArray();
    }
}
