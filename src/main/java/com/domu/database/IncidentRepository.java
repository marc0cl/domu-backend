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
                INSERT INTO incidents (user_id, unit_id, building_id, title, description, category, priority, status, assigned_to_user_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            if (incident.buildingId() != null) {
                statement.setLong(3, incident.buildingId());
            } else {
                statement.setNull(3, java.sql.Types.BIGINT);
            }
            statement.setString(4, incident.title());
            statement.setString(5, incident.description());
            statement.setString(6, incident.category());
            statement.setString(7, incident.priority());
            statement.setString(8, incident.status());
            if (incident.assignedToUserId() != null) {
                statement.setLong(9, incident.assignedToUserId());
            } else {
                statement.setNull(9, java.sql.Types.BIGINT);
            }
            statement.setTimestamp(10, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return new IncidentRow(
                            id,
                            incident.userId(),
                            incident.unitId(),
                            incident.buildingId(),
                            incident.title(),
                            incident.description(),
                            incident.category(),
                            incident.priority(),
                            incident.status(),
                            incident.assignedToUserId(),
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
        String sql = baseQuery(from, to, false, false);
        return executeQuery(sql, null, null, from, to);
    }

    public List<IncidentRow> findByUser(Long userId, LocalDateTime from, LocalDateTime to) {
        String sql = baseQuery(from, to, true, false);
        return executeQuery(sql, userId, null, from, to);
    }

    /**
     * Encuentra incidentes filtrados por edificio.
     * Usa la columna building_id directamente (sin JOIN).
     */
    public List<IncidentRow> findByBuilding(Long buildingId, LocalDateTime from, LocalDateTime to) {
        String sql = baseQuery(from, to, false, true);
        return executeQuery(sql, null, buildingId, from, to);
    }

    public Optional<IncidentRow> findById(Long id) {
        String sql = """
                SELECT id, user_id, unit_id, building_id, title, description, category, priority, status, assigned_to_user_id, created_at, updated_at
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

    public IncidentRow updateAssignment(Long id, Long assignedToUserId, LocalDateTime updatedAt) {
        String sql = """
                UPDATE incidents
                SET assigned_to_user_id = ?, updated_at = ?
                WHERE id = ?
                """;
        LocalDateTime effectiveUpdatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (assignedToUserId != null) {
                statement.setLong(1, assignedToUserId);
            } else {
                statement.setNull(1, java.sql.Types.BIGINT);
            }
            statement.setTimestamp(2, Timestamp.valueOf(effectiveUpdatedAt));
            statement.setLong(3, id);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("Incidente no encontrado para actualizar asignación");
            }
            return findById(id)
                    .orElseThrow(() -> new RepositoryException("No se pudo recuperar el incidente actualizado"));
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando asignación del incidente", e);
        }
    }

    private String baseQuery(LocalDateTime from, LocalDateTime to, boolean filterByUser, boolean filterByBuilding) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT i.id, i.user_id, i.unit_id, i.building_id, i.title, i.description, i.category, i.priority, i.status, i.assigned_to_user_id, i.created_at, i.updated_at ");
        sb.append("FROM incidents i ");
        sb.append("WHERE 1=1 ");
        if (filterByUser) {
            sb.append("AND i.user_id = ? ");
        }
        if (filterByBuilding) {
            // Filtrar directamente por building_id (sin JOIN)
            sb.append("AND i.building_id = ? ");
        }
        if (from != null) {
            sb.append("AND i.created_at >= ? ");
        }
        if (to != null) {
            sb.append("AND i.created_at <= ? ");
        }
        sb.append("ORDER BY i.created_at DESC ");
        return sb.toString();
    }

    private List<IncidentRow> executeQuery(String sql, Long userId, Long buildingId, LocalDateTime from,
            LocalDateTime to) {
        List<IncidentRow> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            int idx = 1;
            if (userId != null) {
                statement.setLong(idx++, userId);
            }
            if (buildingId != null) {
                statement.setLong(idx++, buildingId);
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
                (Long) rs.getObject("building_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("priority"),
                rs.getString("status"),
                (Long) rs.getObject("assigned_to_user_id"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                updated != null ? updated.toLocalDateTime() : null);
    }

    public record IncidentRow(
            Long id,
            Long userId,
            Long unitId,
            Long buildingId,
            String title,
            String description,
            String category,
            String priority,
            String status,
            Long assignedToUserId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
