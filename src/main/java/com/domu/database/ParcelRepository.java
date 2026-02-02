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

public class ParcelRepository {

    private final DataSource dataSource;

    @Inject
    public ParcelRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ParcelRow insert(ParcelRow parcel) {
        String sql = """
                INSERT INTO parcels
                (building_id, unit_id, received_by_user_id, sender, description, status, received_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime receivedAt = parcel.receivedAt() != null ? parcel.receivedAt() : LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, parcel.buildingId());
            statement.setLong(2, parcel.unitId());
            statement.setLong(3, parcel.receivedByUserId());
            statement.setString(4, parcel.sender());
            statement.setString(5, parcel.description());
            statement.setString(6, parcel.status());
            statement.setTimestamp(7, Timestamp.valueOf(receivedAt));
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setTimestamp(9, Timestamp.valueOf(now));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return findById(id)
                            .orElseThrow(() -> new RepositoryException("No se pudo recuperar la encomienda creada"));
                }
            }
            throw new RepositoryException("No se pudo obtener el id de la encomienda");
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando encomienda", e);
        }
    }

    public Optional<ParcelRow> findById(Long id) {
        String sql = baseSelect() + " WHERE p.id = ?";
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
            throw new RepositoryException("Error obteniendo encomienda", e);
        }
    }

    public List<ParcelRow> findByUnit(Long unitId, String status) {
        StringBuilder sql = new StringBuilder(baseSelect())
                .append(" WHERE p.unit_id = ? ");
        boolean hasStatus = status != null && !status.isBlank();
        if (hasStatus) {
            sql.append(" AND p.status = ? ");
        }
        sql.append(" ORDER BY p.received_at DESC ");
        List<ParcelRow> parcels = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setLong(1, unitId);
            if (hasStatus) {
                statement.setString(2, status);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    parcels.add(mapRow(rs));
                }
            }
            return parcels;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo encomiendas por unidad", e);
        }
    }

    public List<ParcelRow> findByBuildingAndUnitNumber(Long buildingId, String unitNumber, String status) {
        StringBuilder sql = new StringBuilder(baseSelect())
                .append(" WHERE p.building_id = ? AND hu.number = ? ");
        boolean hasStatus = status != null && !status.isBlank();
        if (hasStatus) {
            sql.append(" AND p.status = ? ");
        }
        sql.append(" ORDER BY p.received_at DESC ");
        List<ParcelRow> parcels = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setLong(index++, buildingId);
            statement.setString(index++, unitNumber);
            if (hasStatus) {
                statement.setString(index++, status);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    parcels.add(mapRow(rs));
                }
            }
            return parcels;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo encomiendas por unidad (numero)", e);
        }
    }

    public List<ParcelRow> findByBuilding(Long buildingId, String status, Long unitId) {
        StringBuilder sql = new StringBuilder(baseSelect())
                .append(" WHERE p.building_id = ? ");
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasUnit = unitId != null;
        if (hasStatus) {
            sql.append(" AND p.status = ? ");
        }
        if (hasUnit) {
            sql.append(" AND p.unit_id = ? ");
        }
        sql.append(" ORDER BY p.received_at DESC ");

        List<ParcelRow> parcels = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setLong(index++, buildingId);
            if (hasStatus) {
                statement.setString(index++, status);
            }
            if (hasUnit) {
                statement.setLong(index++, unitId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    parcels.add(mapRow(rs));
                }
            }
            return parcels;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo encomiendas por edificio", e);
        }
    }

    public ParcelRow updateStatus(Long parcelId, String status, Long retrievedByUserId, LocalDateTime retrievedAt,
            Long buildingId) {
        String sql = """
                UPDATE parcels
                SET status = ?, retrieved_at = ?, retrieved_by_user_id = ?, updated_at = NOW()
                WHERE id = ? AND building_id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            if (retrievedAt != null) {
                statement.setTimestamp(2, Timestamp.valueOf(retrievedAt));
            } else {
                statement.setNull(2, java.sql.Types.TIMESTAMP);
            }
            if (retrievedByUserId != null) {
                statement.setLong(3, retrievedByUserId);
            } else {
                statement.setNull(3, java.sql.Types.BIGINT);
            }
            statement.setLong(4, parcelId);
            statement.setLong(5, buildingId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No se pudo actualizar la encomienda");
            }
            return findById(parcelId)
                    .orElseThrow(() -> new RepositoryException("No se pudo recuperar la encomienda actualizada"));
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando encomienda", e);
        }
    }

    public ParcelRow update(ParcelRow parcel) {
        String sql = """
                UPDATE parcels
                SET unit_id = ?, sender = ?, description = ?, received_at = ?, updated_at = NOW()
                WHERE id = ? AND building_id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parcel.unitId());
            statement.setString(2, parcel.sender());
            statement.setString(3, parcel.description());
            if (parcel.receivedAt() != null) {
                statement.setTimestamp(4, Timestamp.valueOf(parcel.receivedAt()));
            } else {
                statement.setNull(4, java.sql.Types.TIMESTAMP);
            }
            statement.setLong(5, parcel.id());
            statement.setLong(6, parcel.buildingId());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No se pudo actualizar la encomienda");
            }
            return findById(parcel.id())
                    .orElseThrow(() -> new RepositoryException("No se pudo recuperar la encomienda actualizada"));
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando encomienda", e);
        }
    }

    public void delete(Long parcelId, Long buildingId) {
        String sql = "DELETE FROM parcels WHERE id = ? AND building_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parcelId);
            statement.setLong(2, buildingId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No se pudo eliminar la encomienda");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error eliminando encomienda", e);
        }
    }

    private String baseSelect() {
        return """
                SELECT p.id, p.building_id, p.unit_id, p.received_by_user_id, p.retrieved_by_user_id,
                       p.sender, p.description, p.status, p.received_at, p.retrieved_at, p.created_at, p.updated_at,
                       hu.number AS unit_number, hu.tower AS unit_tower, hu.floor AS unit_floor
                FROM parcels p
                JOIN housing_units hu ON hu.id = p.unit_id
                """;
    }

    private ParcelRow mapRow(ResultSet rs) throws SQLException {
        return new ParcelRow(
                rs.getLong("id"),
                rs.getLong("building_id"),
                rs.getLong("unit_id"),
                rs.getString("unit_number"),
                rs.getString("unit_tower"),
                rs.getString("unit_floor"),
                rs.getLong("received_by_user_id"),
                (Long) rs.getObject("retrieved_by_user_id"),
                rs.getString("sender"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getTimestamp("received_at") != null ? rs.getTimestamp("received_at").toLocalDateTime() : null,
                rs.getTimestamp("retrieved_at") != null ? rs.getTimestamp("retrieved_at").toLocalDateTime() : null,
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
    }

    public record ParcelRow(
            Long id,
            Long buildingId,
            Long unitId,
            String unitNumber,
            String unitTower,
            String unitFloor,
            Long receivedByUserId,
            Long retrievedByUserId,
            String sender,
            String description,
            String status,
            LocalDateTime receivedAt,
            LocalDateTime retrievedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
