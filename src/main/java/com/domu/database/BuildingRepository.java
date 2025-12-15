package com.domu.database;

import com.domu.domain.BuildingRequest;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class BuildingRepository {

    private final DataSource dataSource;

    @Inject
    public BuildingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public BuildingRequest insertRequest(BuildingRequest request) {
        String sql = """
                INSERT INTO building_requests
                (requested_by_user_id, name, tower_label, address, commune, city, admin_phone, admin_email, admin_name, admin_document, floors, units_count, latitude, longitude, proof_text, box_folder_id, box_file_id, box_file_name, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, request.requestedByUserId());
            statement.setString(2, request.name());
            statement.setString(3, request.towerLabel());
            statement.setString(4, request.address());
            statement.setString(5, request.commune());
            statement.setString(6, request.city());
            statement.setString(7, request.adminPhone());
            statement.setString(8, request.adminEmail());
            statement.setString(9, request.adminName());
            statement.setString(10, request.adminDocument());
            if (request.floors() != null) {
                statement.setInt(11, request.floors());
            } else {
                statement.setNull(11, java.sql.Types.INTEGER);
            }
            if (request.unitsCount() != null) {
                statement.setInt(12, request.unitsCount());
            } else {
                statement.setNull(12, java.sql.Types.INTEGER);
            }
            if (request.latitude() != null) {
                statement.setDouble(13, request.latitude());
            } else {
                statement.setNull(13, java.sql.Types.DOUBLE);
            }
            if (request.longitude() != null) {
                statement.setDouble(14, request.longitude());
            } else {
                statement.setNull(14, java.sql.Types.DOUBLE);
            }
            statement.setString(15, request.proofText());
            statement.setString(16, request.boxFolderId());
            statement.setString(17, request.boxFileId());
            statement.setString(18, request.boxFileName());
            statement.setString(19, request.status());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return request.withId(id);
                }
            }
            throw new RepositoryException("No se pudo obtener el id de la solicitud de edificio");
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando solicitud de edificio", e);
        }
    }

    public Optional<BuildingRequest> findRequest(Long requestId) {
        String sql = """
                SELECT id, requested_by_user_id, name, tower_label, address, commune, city, admin_phone, admin_email,
                       admin_name, admin_document, floors, units_count, latitude, longitude, proof_text, box_folder_id, box_file_id, box_file_name, status,
                       created_at, reviewed_by_user_id, reviewed_at, review_notes, building_id
                FROM building_requests
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requestId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRequest(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo solicitud de edificio", e);
        }
    }

    public void updateBoxMetadata(Long requestId, String folderId, String fileId, String fileName) {
        String sql = """
                UPDATE building_requests
                SET box_folder_id = ?,
                    box_file_id = ?,
                    box_file_name = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, folderId);
            statement.setString(2, fileId);
            statement.setString(3, fileName);
            statement.setLong(4, requestId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando metadatos de Box", e);
        }
    }

    public Long insertBuildingFromRequest(BuildingRequest request, Long ownerUserId) {
        String sql = """
                INSERT INTO buildings (name, address, commune, city, admin_phone, admin_email, floors, units_count, tower_label, owner_user_id, latitude, longitude, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, request.name());
            statement.setString(2, request.address());
            statement.setString(3, request.commune());
            statement.setString(4, request.city());
            statement.setString(5, request.adminPhone());
            statement.setString(6, request.adminEmail());
            if (request.floors() != null) {
                statement.setInt(7, request.floors());
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            if (request.unitsCount() != null) {
                statement.setInt(8, request.unitsCount());
            } else {
                statement.setNull(8, java.sql.Types.INTEGER);
            }
            statement.setString(9, request.towerLabel());
            if (ownerUserId != null) {
                statement.setLong(10, ownerUserId);
            } else {
                statement.setNull(10, java.sql.Types.BIGINT);
            }
            if (request.latitude() != null) {
                statement.setDouble(11, request.latitude());
            } else {
                statement.setNull(11, java.sql.Types.DOUBLE);
            }
            if (request.longitude() != null) {
                statement.setDouble(12, request.longitude());
            } else {
                statement.setNull(12, java.sql.Types.DOUBLE);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new RepositoryException("No se pudo obtener el id del edificio creado");
        } catch (SQLException e) {
            throw new RepositoryException("Error creando edificio desde solicitud", e);
        }
    }

    public void approveRequest(Long requestId, Long reviewerUserId, String reviewNotes, Long buildingId) {
        String sql = """
                UPDATE building_requests
                SET status = 'APPROVED',
                    reviewed_by_user_id = ?,
                    reviewed_at = ?,
                    review_notes = ?,
                    building_id = ?
                WHERE id = ? AND status = 'PENDING'
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reviewerUserId);
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(3, reviewNotes);
            if (buildingId != null) {
                statement.setLong(4, buildingId);
            } else {
                statement.setNull(4, java.sql.Types.BIGINT);
            }
            statement.setLong(5, requestId);
            Integer updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No se pudo aprobar la solicitud (¿ya aprobada o inexistente?)");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error aprobando solicitud de edificio", e);
        }
    }

    public Long findBuildingIdByUnitId(Long unitId) {
        String sql = "SELECT building_id FROM housing_units WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, unitId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("building_id");
                }
            }
            return null;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo edificio por unidad", e);
        }
    }

    private BuildingRequest mapRequest(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long requestedBy = rs.getLong("requested_by_user_id");
        String name = rs.getString("name");
        String towerLabel = rs.getString("tower_label");
        String address = rs.getString("address");
        String commune = rs.getString("commune");
        String city = rs.getString("city");
        String adminPhone = rs.getString("admin_phone");
        String adminEmail = rs.getString("admin_email");
        String adminName = rs.getString("admin_name");
        String adminDocument = rs.getString("admin_document");
        Integer floors = (Integer) rs.getObject("floors");
        Integer unitsCount = (Integer) rs.getObject("units_count");
        Double latitude = (Double) rs.getObject("latitude");
        Double longitude = (Double) rs.getObject("longitude");
        String proofText = rs.getString("proof_text");
        String boxFolderId = rs.getString("box_folder_id");
        String boxFileId = rs.getString("box_file_id");
        String boxFileName = rs.getString("box_file_name");
        String status = rs.getString("status");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        Long reviewedBy = (Long) rs.getObject("reviewed_by_user_id");
        java.sql.Timestamp reviewedAtRaw = rs.getTimestamp("reviewed_at");
        LocalDateTime reviewedAt = reviewedAtRaw != null ? reviewedAtRaw.toLocalDateTime() : null;
        String reviewNotes = rs.getString("review_notes");
        Long buildingId = (Long) rs.getObject("building_id");

        return new BuildingRequest(
                id,
                requestedBy,
                name,
                towerLabel,
                address,
                commune,
                city,
                adminPhone,
                adminEmail,
                adminName,
                adminDocument,
                floors,
                unitsCount,
                latitude,
                longitude,
                proofText,
                boxFolderId,
                boxFileId,
                boxFileName,
                status,
                createdAt,
                reviewedBy,
                reviewedAt,
                reviewNotes,
                buildingId
        );
    }
}

