package com.domu.service;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIResponseException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.domu.config.AppConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified Box storage service.
 * Uses Box Java SDK with Developer Token for file storage.
 * Replaces GcsStorageService — all files are stored in Box folders.
 */
@Singleton
public class BoxStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoxStorageService.class);

    private final BoxAPIConnection api;
    private final String rootFolderId;
    private final Map<String, String> folderIdCache = new ConcurrentHashMap<>();

    @Inject
    public BoxStorageService(AppConfig config) {
        String token = config.boxDeveloperToken();
        this.rootFolderId = config.boxRootFolderId() != null && !config.boxRootFolderId().isBlank()
                ? config.boxRootFolderId() : "0";

        if (token == null || token.isBlank()) {
            LOGGER.warn("BOX_TOKEN is empty — Box uploads will fail. Set a valid developer token.");
            this.api = null;
        } else {
            this.api = new BoxAPIConnection(token);
            LOGGER.info("BoxStorageService initialized — rootFolderId={}", rootFolderId);
        }
    }

    // ─── Upload ─────────────────────────────────────────────────────────

    /**
     * Uploads a file to Box under the given folder path.
     *
     * @param folderPath relative folder path (e.g. "profiles/42")
     * @param fileName   the file name (e.g. "avatar.jpg")
     * @param content    file bytes
     * @param contentType content type (used for logging only; Box infers type)
     * @return the Box file ID of the uploaded file
     */
    public String upload(String folderPath, String fileName, byte[] content, String contentType) {
        ensureConnected();

        String folderId = getOrCreateFolder(folderPath);
        BoxFolder folder = new BoxFolder(api, folderId);

        // Check if file already exists in the folder — if so, upload new version
        for (BoxItem.Info itemInfo : folder) {
            if (itemInfo instanceof BoxFile.Info fileInfo && fileName.equals(fileInfo.getName())) {
                BoxFile existingFile = fileInfo.getResource();
                existingFile.uploadNewVersion(new ByteArrayInputStream(content));
                LOGGER.debug("Updated existing file in Box: {}/{} (fileId={})", folderPath, fileName, fileInfo.getID());
                return fileInfo.getID();
            }
        }

        // File doesn't exist — upload new
        BoxFile.Info uploadedFile = folder.uploadFile(new ByteArrayInputStream(content), fileName);
        String fileId = uploadedFile.getID();
        LOGGER.debug("Uploaded new file to Box: {}/{} (fileId={}, {} bytes, type={})",
                folderPath, fileName, fileId, content.length, contentType);
        return fileId;
    }

    /**
     * Convenience upload that takes a single objectPath (folderPath + fileName combined).
     * Splits the path into folder and file name.
     *
     * @return the Box file ID
     */
    public String upload(String objectPath, byte[] content, String contentType) {
        int lastSlash = objectPath.lastIndexOf('/');
        String folderPath;
        String fileName;
        if (lastSlash > 0) {
            folderPath = objectPath.substring(0, lastSlash);
            fileName = objectPath.substring(lastSlash + 1);
        } else {
            folderPath = "";
            fileName = objectPath;
        }
        return upload(folderPath, fileName, content, contentType);
    }

    // ─── Download ───────────────────────────────────────────────────────

    /**
     * Downloads file content from Box by file ID.
     */
    public byte[] download(String fileId) {
        ensureConnected();
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        try {
            BoxFile file = new BoxFile(api, fileId);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            file.download(out);
            return out.toByteArray();
        } catch (Exception e) {
            LOGGER.error("Error downloading file from Box (fileId={}): {}", fileId, e.getMessage());
            return null;
        }
    }

    // ─── Delete ─────────────────────────────────────────────────────────

    /**
     * Deletes a file from Box by file ID.
     */
    public void delete(String fileId) {
        ensureConnected();
        if (fileId == null || fileId.isBlank()) {
            return;
        }
        try {
            BoxFile file = new BoxFile(api, fileId);
            file.delete();
            LOGGER.debug("Deleted file from Box: fileId={}", fileId);
        } catch (Exception e) {
            LOGGER.error("Error deleting file from Box (fileId={}): {}", fileId, e.getMessage());
        }
    }

    // ─── URL resolution ─────────────────────────────────────────────────

    /**
     * Generates a proxy URL for serving Box files through the backend.
     */
    public String resolveUrl(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        return "/api/files/" + fileId + "/download";
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

    // ─── Folder management ──────────────────────────────────────────────

    /**
     * Navigates or creates the folder hierarchy under root, returning the leaf folder ID.
     * Results are cached to avoid repeated API calls.
     *
     * @param path e.g. "profiles/42" or "marketplace/1/100"
     * @return Box folder ID of the deepest folder
     */
    String getOrCreateFolder(String path) {
        if (path == null || path.isBlank()) {
            return rootFolderId;
        }

        // Check cache first
        String cached = folderIdCache.get(path);
        if (cached != null) {
            return cached;
        }

        String[] segments = path.split("/");
        String currentFolderId = rootFolderId;
        StringBuilder currentPath = new StringBuilder();

        for (String segment : segments) {
            if (segment.isBlank()) continue;

            if (currentPath.length() > 0) {
                currentPath.append("/");
            }
            currentPath.append(segment);

            String pathKey = currentPath.toString();
            String cachedId = folderIdCache.get(pathKey);
            if (cachedId != null) {
                currentFolderId = cachedId;
                continue;
            }

            // Look for existing subfolder
            BoxFolder parentFolder = new BoxFolder(api, currentFolderId);
            String foundId = null;
            for (BoxItem.Info childInfo : parentFolder) {
                if (childInfo instanceof BoxFolder.Info folderInfo && segment.equals(folderInfo.getName())) {
                    foundId = folderInfo.getID();
                    break;
                }
            }

            if (foundId == null) {
                // Create the subfolder — handle 409 conflict if it already exists
                try {
                    BoxFolder.Info created = parentFolder.createFolder(segment);
                    foundId = created.getID();
                    LOGGER.debug("Created Box folder: {} (id={})", pathKey, foundId);
                } catch (BoxAPIResponseException e) {
                    if (e.getResponseCode() == 409) {
                        // Folder already exists — extract ID from 409 response body
                        foundId = extractConflictFolderId(e.getResponse(), segment);
                        if (foundId != null) {
                            LOGGER.debug("Folder '{}' already existed (id={})", pathKey, foundId);
                        } else {
                            throw new ValidationException("No se pudo resolver la carpeta: " + pathKey);
                        }
                    } else {
                        throw e;
                    }
                }
            }

            folderIdCache.put(pathKey, foundId);
            currentFolderId = foundId;
        }

        return currentFolderId;
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    /**
     * Extracts the existing folder ID from a Box 409 conflict response.
     * Response format: {"context_info":{"conflicts":[{"id":"12345","type":"folder","name":"..."}]}}
     */
    private String extractConflictFolderId(String responseBody, String expectedName) {
        if (responseBody == null) return null;
        try {
            // Simple JSON parsing — look for "id" in conflicts array
            int conflictsIdx = responseBody.indexOf("\"conflicts\"");
            if (conflictsIdx < 0) return null;

            // Find objects within conflicts array
            int arrStart = responseBody.indexOf('[', conflictsIdx);
            if (arrStart < 0) return null;

            // Extract id from the first conflict object
            int idIdx = responseBody.indexOf("\"id\"", arrStart);
            if (idIdx < 0) return null;

            // Parse the id value: "id":"12345" or "id": "12345"
            int colonIdx = responseBody.indexOf(':', idIdx + 4);
            if (colonIdx < 0) return null;
            int firstQuote = responseBody.indexOf('"', colonIdx + 1);
            if (firstQuote < 0) return null;
            int secondQuote = responseBody.indexOf('"', firstQuote + 1);
            if (secondQuote < 0) return null;

            return responseBody.substring(firstQuote + 1, secondQuote);
        } catch (Exception ex) {
            LOGGER.warn("Could not parse 409 conflict response for folder '{}': {}", expectedName, ex.getMessage());
            return null;
        }
    }

    private void ensureConnected() {
        if (api == null) {
            throw new ValidationException("Box no está configurado. Verifica BOX_TOKEN en la configuración.");
        }
    }

    /**
     * Returns the content type based on file extension.
     * Used by the proxy endpoint to set the response Content-Type header.
     */
    public String guessContentType(String fileId) {
        if (fileId == null) return "application/octet-stream";
        try {
            BoxFile file = new BoxFile(api, fileId);
            BoxFile.Info info = file.getInfo("name");
            String name = info.getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
                if (lower.endsWith(".png")) return "image/png";
                if (lower.endsWith(".gif")) return "image/gif";
                if (lower.endsWith(".webp")) return "image/webp";
                if (lower.endsWith(".pdf")) return "application/pdf";
                if (lower.endsWith(".mp3")) return "audio/mpeg";
                if (lower.endsWith(".ogg")) return "audio/ogg";
                if (lower.endsWith(".wav")) return "audio/wav";
                if (lower.endsWith(".webm")) return "audio/webm";
                if (lower.endsWith(".m4a")) return "audio/mp4";
            }
        } catch (Exception e) {
            LOGGER.warn("Could not determine content type for fileId={}: {}", fileId, e.getMessage());
        }
        return "application/octet-stream";
    }
}
