package com.domu.database;

import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VisitRepository {

    private final DataSource dataSource;

    @Inject
    public VisitRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public VisitRow insertVisit(VisitRow visit) {
        String sql = "INSERT INTO visits (visitor_name, visitor_document, visitor_type, company, created_at) VALUES (?, ?, ?, ?, ?)";
        LocalDateTime createdAt = visit.createdAt() != null ? visit.createdAt() : LocalDateTime.now();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, visit.visitorName());
            if (visit.visitorDocument() != null && !visit.visitorDocument().isBlank()) {
                statement.setString(2, visit.visitorDocument());
            } else {
                statement.setNull(2, java.sql.Types.VARCHAR);
            }
            statement.setString(3, visit.visitorType() != null ? visit.visitorType() : "VISIT");
            if (visit.company() != null && !visit.company().isBlank()) {
                statement.setString(4, visit.company());
            } else {
                statement.setNull(4, java.sql.Types.VARCHAR);
            }
            statement.setTimestamp(5, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return new VisitRow(
                            id,
                            visit.visitorName(),
                            visit.visitorDocument(),
                            visit.visitorType(),
                            visit.company(),
                            createdAt
                    );
                }
            }
            throw new RepositoryException("No se pudo obtener el id de la visita");
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando visita", e);
        }
    }

    public VisitAuthorizationRow insertAuthorization(VisitAuthorizationRow authorization) {
        String sql = """
                INSERT INTO visit_authorizations
                (visit_id, resident_user_id, unit_id, valid_from, valid_until, status, qr_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime createdAt = authorization.createdAt() != null ? authorization.createdAt() : LocalDateTime.now();
        String status = authorization.status() != null ? authorization.status() : "SCHEDULED";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, authorization.visitId());
            statement.setLong(2, authorization.residentUserId());
            statement.setLong(3, authorization.unitId());
            statement.setTimestamp(4, Timestamp.valueOf(authorization.validFrom()));
            statement.setTimestamp(5, Timestamp.valueOf(authorization.validUntil()));
            statement.setString(6, status);
            if (authorization.qrHash() != null && !authorization.qrHash().isBlank()) {
                statement.setString(7, authorization.qrHash());
            } else {
                statement.setNull(7, java.sql.Types.VARCHAR);
            }
            statement.setTimestamp(8, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return new VisitAuthorizationRow(
                            id,
                            authorization.visitId(),
                            authorization.residentUserId(),
                            authorization.unitId(),
                            authorization.validFrom(),
                            authorization.validUntil(),
                            status,
                            authorization.qrHash(),
                            createdAt
                    );
                }
            }
            throw new RepositoryException("No se pudo obtener el id de la autorización de visita");
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando autorización de visita", e);
        }
    }

    public void updateAuthorizationStatus(Long authorizationId, String status) {
        String sql = "UPDATE visit_authorizations SET status = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, authorizationId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No se encontró la autorización para actualizar su estado");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando estado de autorización de visita", e);
        }
    }

    public AccessLogRow insertAccessLog(AccessLogRow log) {
        String sql = """
                INSERT INTO visit_access_logs (visit_id, authorization_id, recorded_at, door, authorized_by_user_id, outcome, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime recordedAt = log.recordedAt() != null ? log.recordedAt() : LocalDateTime.now();
        LocalDateTime createdAt = log.createdAt() != null ? log.createdAt() : recordedAt;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, log.visitId());
            if (log.authorizationId() != null) {
                statement.setLong(2, log.authorizationId());
            } else {
                statement.setNull(2, java.sql.Types.BIGINT);
            }
            statement.setTimestamp(3, Timestamp.valueOf(recordedAt));
            if (log.door() != null && !log.door().isBlank()) {
                statement.setString(4, log.door());
            } else {
                statement.setNull(4, java.sql.Types.VARCHAR);
            }
            if (log.authorizedByUserId() != null) {
                statement.setLong(5, log.authorizedByUserId());
            } else {
                statement.setNull(5, java.sql.Types.BIGINT);
            }
            statement.setString(6, log.outcome());
            statement.setTimestamp(7, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return new AccessLogRow(
                            id,
                            log.visitId(),
                            log.authorizationId(),
                            recordedAt,
                            log.door(),
                            log.authorizedByUserId(),
                            log.outcome(),
                            createdAt
                    );
                }
            }
            throw new RepositoryException("No se pudo obtener el id del registro de acceso");
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando registro de acceso de visita", e);
        }
    }

    public Optional<VisitSummaryRow> findAuthorizationForResident(Long authorizationId, Long residentUserId) {
        String sql = baseSummaryQuery() + " WHERE va.id = ? AND va.resident_user_id = ? ORDER BY va.created_at DESC";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, authorizationId);
            statement.setLong(2, residentUserId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapSummary(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo autorización de visita", e);
        }
    }

    public Optional<VisitSummaryRow> findAuthorization(Long authorizationId) {
        String sql = baseSummaryQuery() + " WHERE va.id = ? ORDER BY va.created_at DESC";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, authorizationId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapSummary(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo autorización de visita", e);
        }
    }

    public List<VisitSummaryRow> findAuthorizationsForResident(Long residentUserId) {
        String sql = baseSummaryQuery() + " WHERE va.resident_user_id = ? ORDER BY va.created_at DESC";
        List<VisitSummaryRow> visits = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, residentUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    visits.add(mapSummary(rs));
                }
            }
            return visits;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo visitas del residente", e);
        }
    }

    private String baseSummaryQuery() {
        return """
                SELECT va.id AS authorization_id,
                       va.visit_id,
                       va.resident_user_id,
                       va.unit_id,
                       va.valid_from,
                       va.valid_until,
                       va.status,
                       va.qr_hash,
                       va.created_at AS authorization_created_at,
                       v.visitor_name,
                       v.visitor_document,
                       v.visitor_type,
                       v.company,
                       v.created_at AS visit_created_at,
                       ci.check_in_at
                FROM visit_authorizations va
                JOIN visits v ON v.id = va.visit_id
                LEFT JOIN (
                    SELECT authorization_id, MAX(recorded_at) AS check_in_at
                    FROM visit_access_logs
                    WHERE outcome = 'CHECK_IN'
                    GROUP BY authorization_id
                ) ci ON ci.authorization_id = va.id
                """;
    }

    private VisitSummaryRow mapSummary(ResultSet rs) throws SQLException {
        LocalDateTime validFrom = rs.getTimestamp("valid_from").toLocalDateTime();
        LocalDateTime validUntil = rs.getTimestamp("valid_until").toLocalDateTime();
        Timestamp checkInTs = rs.getTimestamp("check_in_at");
        LocalDateTime checkInAt = checkInTs != null ? checkInTs.toLocalDateTime() : null;
        return new VisitSummaryRow(
                rs.getLong("authorization_id"),
                rs.getLong("visit_id"),
                rs.getLong("resident_user_id"),
                rs.getLong("unit_id"),
                rs.getString("visitor_name"),
                rs.getString("visitor_document"),
                rs.getString("visitor_type"),
                validFrom,
                validUntil,
                rs.getString("status"),
                rs.getTimestamp("authorization_created_at").toLocalDateTime(),
                checkInAt
        );
    }

    public record VisitRow(
            Long id,
            String visitorName,
            String visitorDocument,
            String visitorType,
            String company,
            LocalDateTime createdAt
    ) {
    }

    public record VisitAuthorizationRow(
            Long id,
            Long visitId,
            Long residentUserId,
            Long unitId,
            LocalDateTime validFrom,
            LocalDateTime validUntil,
            String status,
            String qrHash,
            LocalDateTime createdAt
    ) {
    }

    public record AccessLogRow(
            Long id,
            Long visitId,
            Long authorizationId,
            LocalDateTime recordedAt,
            String door,
            Long authorizedByUserId,
            String outcome,
            LocalDateTime createdAt
    ) {
    }

    public record VisitSummaryRow(
            Long authorizationId,
            Long visitId,
            Long residentUserId,
            Long unitId,
            String visitorName,
            String visitorDocument,
            String visitorType,
            LocalDateTime validFrom,
            LocalDateTime validUntil,
            String status,
            LocalDateTime createdAt,
            LocalDateTime checkInAt
    ) {
    }
}

