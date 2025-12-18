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

public class IncidentRepository {

    private final DataSource dataSource;

    @Inject
    public IncidentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public IncidentRow insert(IncidentRow incident) {
        String sql = """
                INSERT INTO incidents (user_id, unit_id, title, description, category, priority, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime createdAt = incident.createdAt() != null ? incident.createdAt() : LocalDateTime.now();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, incident.userId());
            if (incident.unitId() != null) {
                statement.setLong(2, incident.unitId());
            } else {
                statement.setNull(2, java.sql.Types.BIGINT);
            }
            statement.setString(3, incident.title());
            statement.setString(4, incident.description());
            statement.setString(5, incident.category());
            statement.setString(6, incident.priority());
            statement.setString(7, incident.status());
            statement.setTimestamp(8, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return new IncidentRow(
                            id,
                            incident.userId(),
                            incident.unitId(),
                            incident.title(),
                            incident.description(),
                            incident.category(),
                            incident.priority(),
                            incident.status(),
                            createdAt,
                            createdAt);
                }
            }
            throw new RepositoryException("No se pudo obtener el id del incidente");
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando incidente", e);
        }
    }

    public List<IncidentRow> findAll(LocalDateTime from, LocalDateTime to) {
        String sql = baseQuery(from, to, false);
        return executeQuery(sql, null, from, to);
    }

    public List<IncidentRow> findByUser(Long userId, LocalDateTime from, LocalDateTime to) {
        String sql = baseQuery(from, to, true);
        return executeQuery(sql, userId, from, to);
    }

    public Optional<IncidentRow> findById(Long id) {
        String sql = """
                SELECT id, user_id, unit_id, title, description, category, priority, status, created_at, updated_at
                FROM incidents
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo incidente", e);
        }
    }

    public IncidentRow updateStatus(Long id, String status, LocalDateTime updatedAt) {
        String sql = """
                UPDATE incidents
                SET status = ?, updated_at = ?
                WHERE id = ?
                """;
        LocalDateTime effectiveUpdatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setTimestamp(2, Timestamp.valueOf(effectiveUpdatedAt));
            statement.setLong(3, id);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("Incidente no encontrado para actualizar estado");
            }
            return findById(id)
                    .orElseThrow(() -> new RepositoryException("No se pudo recuperar el incidente actualizado"));
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando estado del incidente", e);
        }
    }

    private String baseQuery(LocalDateTime from, LocalDateTime to, boolean filterByUser) {
        StringBuilder sb = new StringBuilder("""
                SELECT id, user_id, unit_id, title, description, category, priority, status, created_at, updated_at
                FROM incidents
                WHERE 1=1
                """);
        if (filterByUser) {
            sb.append(" AND user_id = ? ");
        }
        if (from != null) {
            sb.append(" AND created_at >= ? ");
        }
        if (to != null) {
            sb.append(" AND created_at <= ? ");
        }
        sb.append(" ORDER BY created_at DESC ");
        return sb.toString();
    }

    private List<IncidentRow> executeQuery(String sql, Long userId, LocalDateTime from, LocalDateTime to) {
        List<IncidentRow> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            Integer idx = 1;
            if (userId != null) {
                statement.setLong(idx++, userId);
            }
            if (from != null) {
                statement.setTimestamp(idx++, Timestamp.valueOf(from));
            }
            if (to != null) {
                statement.setTimestamp(idx++, Timestamp.valueOf(to));
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo incidentes", e);
        }
    }

    private IncidentRow mapRow(ResultSet rs) throws SQLException {
        Timestamp updated = rs.getTimestamp("updated_at");
        return new IncidentRow(
                rs.getLong("id"),
                rs.getLong("user_id"),
                (Long) rs.getObject("unit_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                updated != null ? updated.toLocalDateTime() : null);
    }

    public record IncidentRow(
            Long id,
            Long userId,
            Long unitId,
            String title,
            String description,
            String category,
            String priority,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
