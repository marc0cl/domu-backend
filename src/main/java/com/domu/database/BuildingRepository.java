package com.domu.database;

import com.domu.domain.BuildingRequest;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.math.BigDecimal;
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
                (requested_by_user_id, name, tower_label, address, commune, city, admin_phone, admin_email, admin_name, admin_document, floors, units_count, latitude, longitude, proof_text, box_folder_id, box_file_id, box_file_name, status, approval_code, approval_code_expires_at, approval_code_used_at, approval_action, admin_invite_code, admin_invite_expires_at, admin_invite_used_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (request.requestedByUserId() != null) {
                statement.setLong(1, request.requestedByUserId());
            } else {
                statement.setNull(1, java.sql.Types.BIGINT);
            }
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
            statement.setString(20, request.approvalCode());
            if (request.approvalCodeExpiresAt() != null) {
                statement.setTimestamp(21, Timestamp.valueOf(request.approvalCodeExpiresAt()));
            } else {
                statement.setNull(21, java.sql.Types.TIMESTAMP);
            }
            statement.setNull(22, java.sql.Types.TIMESTAMP);
            statement.setNull(23, java.sql.Types.VARCHAR);
            statement.setString(24, request.adminInviteCode());
            if (request.adminInviteExpiresAt() != null) {
                statement.setTimestamp(25, Timestamp.valueOf(request.adminInviteExpiresAt()));
            } else {
                statement.setNull(25, java.sql.Types.TIMESTAMP);
            }
            statement.setNull(26, java.sql.Types.TIMESTAMP);
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
                       created_at, reviewed_by_user_id, reviewed_at, review_notes, building_id,
                       approval_code, approval_code_expires_at, approval_code_used_at, approval_action,
                       admin_invite_code, admin_invite_expires_at, admin_invite_used_at
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

    public Optional<BuildingRequest> findRequestByApprovalCode(String approvalCode) {
        String sql = """
                SELECT id, requested_by_user_id, name, tower_label, address, commune, city, admin_phone, admin_email,
                       admin_name, admin_document, floors, units_count, latitude, longitude, proof_text, box_folder_id, box_file_id, box_file_name, status,
                       created_at, reviewed_by_user_id, reviewed_at, review_notes, building_id,
                       approval_code, approval_code_expires_at, approval_code_used_at, approval_action,
                       admin_invite_code, admin_invite_expires_at, admin_invite_used_at
                FROM building_requests
                WHERE approval_code = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, approvalCode);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRequest(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo solicitud por código de aprobación", e);
        }
    }

    public Optional<BuildingRequest> findRequestByAdminInviteCode(String inviteCode) {
        String sql = """
                SELECT id, requested_by_user_id, name, tower_label, address, commune, city, admin_phone, admin_email,
                       admin_name, admin_document, floors, units_count, latitude, longitude, proof_text, box_folder_id, box_file_id, box_file_name, status,
                       created_at, reviewed_by_user_id, reviewed_at, review_notes, building_id,
                       approval_code, approval_code_expires_at, approval_code_used_at, approval_action,
                       admin_invite_code, admin_invite_expires_at, admin_invite_used_at
                FROM building_requests
                WHERE admin_invite_code = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, inviteCode);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRequest(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo solicitud por código de invitación", e);
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

    public void updateAdminInvite(Long requestId, String inviteCode, LocalDateTime expiresAt) {
        String sql = """
                UPDATE building_requests
                SET admin_invite_code = ?,
                    admin_invite_expires_at = ?,
                    admin_invite_used_at = NULL
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, inviteCode);
            if (expiresAt != null) {
                statement.setTimestamp(2, Timestamp.valueOf(expiresAt));
            } else {
                statement.setNull(2, java.sql.Types.TIMESTAMP);
            }
            statement.setLong(3, requestId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando invitación de administrador", e);
        }
    }

    public Long insertBuildingFromRequest(BuildingRequest request, Long ownerUserId) {
        String sql = """
                INSERT INTO buildings (name, address, commune, city, admin_phone, admin_email, floors, units_count, tower_label, owner_user_id, latitude, longitude, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Long resolvedOwnerId = resolveOwnerUserId(connection, ownerUserId);
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
            if (resolvedOwnerId != null) {
                statement.setLong(10, resolvedOwnerId);
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

    private Long resolveOwnerUserId(Connection connection, Long ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        String sql = "SELECT 1 FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, ownerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return ownerUserId;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new RepositoryException("Error validando owner_user_id", e);
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

    public void approveRequestByCode(Long requestId, String reviewNotes, Long buildingId) {
        String sql = """
                UPDATE building_requests
                SET status = 'APPROVED',
                    reviewed_by_user_id = NULL,
                    reviewed_at = ?,
                    review_notes = ?,
                    building_id = ?,
                    approval_code_used_at = ?,
                    approval_action = 'EMAIL_APPROVED'
                WHERE id = ? AND status = 'PENDING' AND approval_code_used_at IS NULL
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(2, reviewNotes);
            if (buildingId != null) {
                statement.setLong(3, buildingId);
            } else {
                statement.setNull(3, java.sql.Types.BIGINT);
            }
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            statement.setLong(5, requestId);
            Integer updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No se pudo aprobar la solicitud (¿ya procesada o inexistente?)");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error aprobando solicitud de edificio por correo", e);
        }
    }

    public void rejectRequest(Long requestId, String reviewNotes) {
        String sql = """
                UPDATE building_requests
                SET status = 'REJECTED',
                    reviewed_by_user_id = NULL,
                    reviewed_at = ?,
                    review_notes = ?,
                    approval_code_used_at = ?,
                    approval_action = 'EMAIL_REJECTED'
                WHERE id = ? AND status = 'PENDING' AND approval_code_used_at IS NULL
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(2, reviewNotes);
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.setLong(4, requestId);
            Integer updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No se pudo rechazar la solicitud (¿ya procesada o inexistente?)");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error rechazando solicitud de edificio", e);
        }
    }

    public void markAdminInviteUsed(Long requestId) {
        String sql = """
                UPDATE building_requests
                SET admin_invite_used_at = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            statement.setLong(2, requestId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error marcando invitación de administrador", e);
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
        BigDecimal latitudeRaw = rs.getBigDecimal("latitude");
        BigDecimal longitudeRaw = rs.getBigDecimal("longitude");
        Double latitude = latitudeRaw != null ? latitudeRaw.doubleValue() : null;
        Double longitude = longitudeRaw != null ? longitudeRaw.doubleValue() : null;
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
        String approvalCode = rs.getString("approval_code");
        java.sql.Timestamp approvalExpiresRaw = rs.getTimestamp("approval_code_expires_at");
        LocalDateTime approvalCodeExpiresAt = approvalExpiresRaw != null ? approvalExpiresRaw.toLocalDateTime() : null;
        java.sql.Timestamp approvalUsedRaw = rs.getTimestamp("approval_code_used_at");
        LocalDateTime approvalCodeUsedAt = approvalUsedRaw != null ? approvalUsedRaw.toLocalDateTime() : null;
        String approvalAction = rs.getString("approval_action");
        String adminInviteCode = rs.getString("admin_invite_code");
        java.sql.Timestamp adminInviteExpiresRaw = rs.getTimestamp("admin_invite_expires_at");
        LocalDateTime adminInviteExpiresAt = adminInviteExpiresRaw != null ? adminInviteExpiresRaw.toLocalDateTime() : null;
        java.sql.Timestamp adminInviteUsedRaw = rs.getTimestamp("admin_invite_used_at");
        LocalDateTime adminInviteUsedAt = adminInviteUsedRaw != null ? adminInviteUsedRaw.toLocalDateTime() : null;

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
                buildingId,
                approvalCode,
                approvalCodeExpiresAt,
                approvalCodeUsedAt,
                approvalAction,
                adminInviteCode,
                adminInviteExpiresAt,
                adminInviteUsedAt
        );
    }
}

