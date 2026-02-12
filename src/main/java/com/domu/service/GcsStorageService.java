package com.domu.service;

import com.domu.config.AppConfig;
import com.domu.service.ValidationException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Unified Google Cloud Storage service.
 * Uses Signed URLs so objects don't need to be public.
 */
@Singleton
public class GcsStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcsStorageService.class);
    /** Signed URLs valid for 7 days — images are re-fetched via API on each session. */
    private static final long SIGNED_URL_DURATION_DAYS = 7;

    private final Storage storage;
    private final String bucketName;
    private final ServiceAccountCredentials saCredentials;

    @Inject
    public GcsStorageService(AppConfig config) {
        this.bucketName = config.gcsBucketName();
        this.saCredentials = loadServiceAccountCredentials(config.gcsKeyFilePath());
        this.storage = buildClient(saCredentials);
        LOGGER.info("GcsStorageService initialized – bucket={}, credentials={}", bucketName,
                saCredentials != null ? "SA key loaded" : "FALLBACK (no SA key)");
    }

    private ServiceAccountCredentials loadServiceAccountCredentials(String keyFilePath) {
        try {
            if (keyFilePath != null && !keyFilePath.isBlank()) {
                Path path = Paths.get(keyFilePath).toAbsolutePath();
                LOGGER.info("Looking for GCS key file at: {}", path);
                if (Files.exists(path)) {
                    GoogleCredentials creds = GoogleCredentials.fromStream(new FileInputStream(path.toFile()));
                    if (creds instanceof ServiceAccountCredentials sa) {
                        LOGGER.info("GCS service account credentials loaded successfully");
                        return sa;
                    }
                    LOGGER.warn("Credentials loaded but are NOT ServiceAccountCredentials – signed URLs will use fallback");
                } else {
                    LOGGER.error("GCS key file NOT FOUND at: {} – images will not load correctly", path);
                }
            } else {
                LOGGER.warn("GCS_KEY_FILE_PATH is empty – signed URLs will use public fallback (images may break)");
            }
        } catch (IOException e) {
            LOGGER.error("Error loading GCS service account credentials", e);
        }
        return null;
    }

    private Storage buildClient(ServiceAccountCredentials sa) {
        if (sa != null) {
            return StorageOptions.newBuilder().setCredentials(sa).build().getService();
        }
        LOGGER.warn("No SA credentials – using application-default credentials");
        return StorageOptions.getDefaultInstance().getService();
    }

    // ─── Upload ─────────────────────────────────────────────────────────

    /**
     * Uploads a file to GCS and returns a signed URL for reading.
     */
    public String upload(String objectPath, byte[] content, String contentType) {
        if (bucketName == null || bucketName.isBlank()) {
            throw new ValidationException("Configuración de almacenamiento incompleta (GCS_BUCKET_NAME no definido)");
        }
        BlobId blobId = BlobId.of(bucketName, objectPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .setCacheControl("public, max-age=86400")
                .build();

        storage.create(blobInfo, content);
        LOGGER.debug("Uploaded {} ({} bytes)", objectPath, content.length);
        return signedUrl(objectPath);
    }

    // ─── Download ───────────────────────────────────────────────────────

    public byte[] download(String objectPath) {
        // If objectPath is a full signed URL, extract the path
        String path = extractObjectPath(objectPath);
        BlobId blobId = BlobId.of(bucketName, path);
        var blob = storage.get(blobId);
        if (blob == null || !blob.exists()) {
            return null;
        }
        return blob.getContent();
    }

    // ─── Delete ─────────────────────────────────────────────────────────

    public void delete(String objectPath) {
        String path = extractObjectPath(objectPath);
        BlobId blobId = BlobId.of(bucketName, path);
        storage.delete(blobId);
    }

    // ─── Signed URL ─────────────────────────────────────────────────────

    /**
     * Generates a signed URL valid for 7 days.
     * The SA must have "Service Account Token Creator" or the key file is used directly.
     */
    public String signedUrl(String objectPath) {
        if (saCredentials == null) {
            // Fallback to public URL if no SA key for signing
            return String.format("https://storage.googleapis.com/%s/%s", bucketName, objectPath);
        }
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectPath)).build();
        URL url = storage.signUrl(blobInfo, SIGNED_URL_DURATION_DAYS, TimeUnit.DAYS,
                Storage.SignUrlOption.signWith(saCredentials));
        return url.toString();
    }

    // ─── Path builders ──────────────────────────────────────────────────

    public String profileAvatarPath(Long userId, String extension) {
        return String.format("profiles/%d/avatar.%s", userId, extension);
    }

    public String profilePrivacyAvatarPath(Long userId, String extension) {
        return String.format("profiles/%d/privacy-avatar.%s", userId, extension);
    }

    public String marketImagePath(Long buildingId, Long itemId, String fileName) {
        return String.format("marketplace/%d/%d/%s", buildingId, itemId, fileName);
    }

    public String communityDocPath(Long requestId, String fileName) {
        return String.format("community-docs/%d/%s", requestId, fileName);
    }

    public String receiptPath(Long buildingId, int year, int month, Long unitId, String fileName) {
        return String.format("receipts/%d/%d-%02d/%d/%s", buildingId, year, month, unitId, fileName);
    }

    public String chatAudioPath(Long roomId, String fileName) {
        return String.format("chat-audio/%d/%s", roomId, fileName);
    }

    public String libraryDocPath(Long buildingId, String fileName) {
        return String.format("library/%d/%s", buildingId, fileName);
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    /**
     * If the input looks like a full URL or signed URL, extract the object path.
     * Otherwise return as-is.
     */
    public String extractObjectPath(String input) {
        if (input == null) return "";
        // Handle signed URLs or public URLs
        String prefix = "storage.googleapis.com/" + bucketName + "/";
        int idx = input.indexOf(prefix);
        if (idx >= 0) {
            String path = input.substring(idx + prefix.length());
            // Remove query params (signed URL params)
            int q = path.indexOf('?');
            return q > 0 ? path.substring(0, q) : path;
        }
        return input;
    }
}
