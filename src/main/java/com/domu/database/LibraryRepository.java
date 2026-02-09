package com.domu.database;

import com.domu.domain.LibraryDocument;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LibraryRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryRepository.class);
    private final DataSource dataSource;
    private volatile boolean schemaInitialized = false;

    @Inject
    public LibraryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<LibraryDocument> findAllByBuildingId(Long buildingId) {
        ensureSchema();
        String sql = "SELECT id, building_id, name, category, file_name, file_url, size, upload_date, uploaded_by FROM library_documents WHERE building_id = ? AND status = 'ACTIVE' ORDER BY upload_date DESC";
        List<LibraryDocument> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching library documents", e);
        }
        return result;
    }

    public Optional<LibraryDocument> findById(Long id) {
        ensureSchema();
        String sql = "SELECT id, building_id, name, category, file_name, file_url, size, upload_date, uploaded_by FROM library_documents WHERE id = ? AND status = 'ACTIVE'";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching library document by id", e);
        }
        return Optional.empty();
    }

    public Long save(LibraryDocument doc) {
        ensureSchema();
        String sql = "INSERT INTO library_documents (building_id, name, category, file_name, file_url, size, uploaded_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, doc.buildingId());
            statement.setString(2, doc.name());
            statement.setString(3, doc.category());
            statement.setString(4, doc.fileName());
            statement.setString(5, doc.fileUrl());
            statement.setLong(6, doc.size());
            statement.setLong(7, doc.uploadedBy());

            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new RepositoryException("Creating library document failed, no ID obtained.");
        } catch (SQLException e) {
            throw new RepositoryException("Error saving library document", e);
        }
    }

    public void delete(Long id) {
        ensureSchema();
        String sql = "UPDATE library_documents SET status = 'DELETED' WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error deleting library document", e);
        }
    }

    private LibraryDocument mapRow(ResultSet rs) throws SQLException {
        Timestamp uploadDate = rs.getTimestamp("upload_date");
        return new LibraryDocument(
            rs.getLong("id"),
            rs.getLong("building_id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getString("file_name"),
            rs.getString("file_url"),
            rs.getLong("size"),
            uploadDate != null ? uploadDate.toLocalDateTime() : null,
            rs.getLong("uploaded_by")
        );
    }

    private void ensureSchema() {
        if (schemaInitialized) return;
        synchronized (this) {
            if (schemaInitialized) return;
            applyMigration();
            schemaInitialized = true;
        }
    }

    private void applyMigration() {
        String migrationPath = "/migrations/030_library.sql";
        try (InputStream inputStream = LibraryRepository.class.getResourceAsStream(migrationPath)) {
            if (inputStream == null) {
                LOGGER.warn("Library migration file not found at {}", migrationPath);
                return;
            }
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String[] statements = sql.split(";");
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                for (String raw : statements) {
                    String trimmed = raw.trim();
                    if (!trimmed.isEmpty()) {
                        statement.execute(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            throw new RepositoryException("Error applying library migration", e);
        }
    }
}
