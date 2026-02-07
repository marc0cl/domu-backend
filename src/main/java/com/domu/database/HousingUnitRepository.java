package com.domu.database;

import com.domu.domain.core.HousingUnit;
import com.domu.dto.HousingUnitResponse;
import com.domu.dto.HousingUnitWithResidents;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HousingUnitRepository {

    private final DataSource dataSource;
    private Boolean auditColumnsExist = null; // Cache para evitar múltiples consultas

    @Inject
    public HousingUnitRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Buscar unidades por edificio (excluyendo eliminadas)
     */
    public List<HousingUnit> findByBuildingId(Long buildingId) {
        // Verificar si las columnas de auditoría existen
        boolean hasAuditColumns = checkAuditColumnsExist();
        String sql = hasAuditColumns ? """
                SELECT id, building_id, number, tower, floor, aliquot_percentage,
                       square_meters, status, created_by_user_id, created_at, updated_at
                FROM housing_units
                WHERE building_id = ? AND status != 'DELETED'
                ORDER BY floor, number
                """ : """
                SELECT id, building_id, number, tower, floor, aliquot_percentage,
                       square_meters, status, NULL as created_by_user_id, NULL as created_at, NULL as updated_at
                FROM housing_units
                WHERE building_id = ? AND (status IS NULL OR status != 'DELETED')
                ORDER BY floor, number
                """;
        List<HousingUnit> units = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    units.add(mapRow(rs));
                }
            }
            return units;
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching housing units by building", e);
        }
    }

    /**
     * Buscar unidad por ID
     */
    public Optional<HousingUnit> findById(Long id) {
        boolean hasAuditColumns = checkAuditColumnsExist();
        String sql = hasAuditColumns ? """
                SELECT id, building_id, number, tower, floor, aliquot_percentage,
                       square_meters, status, created_by_user_id, created_at, updated_at
                FROM housing_units
                WHERE id = ?
                """ : """
                SELECT id, building_id, number, tower, floor, aliquot_percentage,
                       square_meters, status, NULL as created_by_user_id, NULL as created_at, NULL as updated_at
                FROM housing_units
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
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching housing unit by id", e);
        }
        return Optional.empty();
    }

    /**
     * Insertar nueva unidad
     */
    public HousingUnit insert(HousingUnit unit) {
        boolean hasAuditColumns = checkAuditColumnsExist();
        String sql = hasAuditColumns ? """
                INSERT INTO housing_units
                (building_id, number, tower, floor, aliquot_percentage, square_meters,
                 status, created_by_user_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """ : """
                INSERT INTO housing_units
                (building_id, number, tower, floor, aliquot_percentage, square_meters, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, unit.buildingId());
            statement.setString(2, unit.number());
            statement.setString(3, unit.tower());
            statement.setString(4, unit.floor());

            if (unit.aliquotPercentage() != null) {
                statement.setBigDecimal(5, unit.aliquotPercentage());
            } else {
                statement.setNull(5, java.sql.Types.DECIMAL);
            }

            if (unit.squareMeters() != null) {
                statement.setBigDecimal(6, unit.squareMeters());
            } else {
                statement.setNull(6, java.sql.Types.DECIMAL);
            }

            statement.setString(7, unit.status() != null ? unit.status() : "ACTIVE");

            if (hasAuditColumns) {
                if (unit.createdByUserId() != null) {
                    statement.setLong(8, unit.createdByUserId());
                } else {
                    statement.setNull(8, java.sql.Types.BIGINT);
                }
            }

            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return findById(id)
                            .orElseThrow(() -> new RepositoryException("Housing unit not found after insert"));
                }
            }
            throw new RepositoryException("No se pudo obtener el id de la unidad creada");
        } catch (SQLException e) {
            throw new RepositoryException("Error inserting housing unit", e);
        }
    }

    /**
     * Actualizar unidad existente
     */
    public HousingUnit update(HousingUnit unit) {
        String sql = """
                UPDATE housing_units
                SET number = ?, tower = ?, floor = ?, aliquot_percentage = ?,
                    square_meters = ?, updated_at = NOW()
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, unit.number());
            statement.setString(2, unit.tower());
            statement.setString(3, unit.floor());

            if (unit.aliquotPercentage() != null) {
                statement.setBigDecimal(4, unit.aliquotPercentage());
            } else {
                statement.setNull(4, java.sql.Types.DECIMAL);
            }

            if (unit.squareMeters() != null) {
                statement.setBigDecimal(5, unit.squareMeters());
            } else {
                statement.setNull(5, java.sql.Types.DECIMAL);
            }

            statement.setLong(6, unit.id());

            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No housing unit updated");
            }
            return findById(unit.id())
                    .orElseThrow(() -> new RepositoryException("Housing unit not found after update"));
        } catch (SQLException e) {
            throw new RepositoryException("Error updating housing unit", e);
        }
    }

    /**
     * Soft delete - cambiar status a DELETED
     */
    public void softDelete(Long id) {
        String sql = "UPDATE housing_units SET status = 'DELETED', updated_at = NOW() WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No housing unit deleted");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error deleting housing unit", e);
        }
    }

    /**
     * Buscar unidad por número dentro de un edificio
     */
    public Optional<HousingUnit> findByBuildingIdAndNumber(Long buildingId, String number) {
        boolean hasAuditColumns = checkAuditColumnsExist();
        String sql = hasAuditColumns ? """
                SELECT id, building_id, number, tower, floor, aliquot_percentage,
                       square_meters, status, created_by_user_id, created_at, updated_at
                FROM housing_units
                WHERE building_id = ? AND number = ? AND status != 'DELETED'
                """ : """
                SELECT id, building_id, number, tower, floor, aliquot_percentage,
                       square_meters, status, NULL as created_by_user_id, NULL as created_at, NULL as updated_at
                FROM housing_units
                WHERE building_id = ? AND number = ? AND (status IS NULL OR status != 'DELETED')
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            statement.setString(2, number);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching housing unit by building and number", e);
        }
        return Optional.empty();
    }

    /**
     * Verificar si existe una unidad con el mismo número en el mismo edificio
     */
    public boolean existsByNumberAndBuildingId(String number, Long buildingId, Long excludeId) {
        String sql = """
                SELECT COUNT(*)
                FROM housing_units
                WHERE number = ? AND building_id = ? AND status != 'DELETED'
                """ + (excludeId != null ? " AND id != ?" : "");

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, number);
            statement.setLong(2, buildingId);
            if (excludeId != null) {
                statement.setLong(3, excludeId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error checking housing unit existence", e);
        }
        return false;
    }

    /**
     * Contar residentes activos en una unidad
     */
    public int countResidentsByUnitId(Long unitId) {
        String sql = """
                SELECT COUNT(*)
                FROM users
                WHERE unit_id = ? AND status = 'ACTIVE'
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, unitId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error counting residents", e);
        }
        return 0;
    }

    /**
     * Buscar unidades con sus residentes
     */
    public List<HousingUnitWithResidents> findByBuildingIdWithResidents(Long buildingId) {
        boolean hasAuditColumns = checkAuditColumnsExist();
        String sql = hasAuditColumns ? """
                SELECT hu.id, hu.building_id, hu.number, hu.tower, hu.floor,
                       hu.aliquot_percentage, hu.square_meters, hu.status,
                       hu.created_by_user_id, hu.created_at, hu.updated_at,
                       CONCAT(creator.first_name, ' ', creator.last_name) as creator_name,
                       u.id as resident_id, u.first_name, u.last_name, u.email,
                       u.phone, u.document_number
                FROM housing_units hu
                LEFT JOIN users creator ON creator.id = hu.created_by_user_id
                LEFT JOIN users u ON u.unit_id = hu.id AND u.status = 'ACTIVE'
                WHERE hu.building_id = ? AND hu.status != 'DELETED'
                ORDER BY hu.floor, hu.number, u.first_name
                """ : """
                SELECT hu.id, hu.building_id, hu.number, hu.tower, hu.floor,
                       hu.aliquot_percentage, hu.square_meters, hu.status,
                       NULL as created_by_user_id, NULL as created_at, NULL as updated_at,
                       NULL as creator_name,
                       u.id as resident_id, u.first_name, u.last_name, u.email,
                       u.phone, u.document_number
                FROM housing_units hu
                LEFT JOIN users u ON u.unit_id = hu.id AND u.status = 'ACTIVE'
                WHERE hu.building_id = ? AND (hu.status IS NULL OR hu.status != 'DELETED')
                ORDER BY hu.floor, hu.number, u.first_name
                """;

        List<HousingUnitWithResidents> result = new ArrayList<>();
        HousingUnitResponse currentUnit = null;
        List<HousingUnitWithResidents.ResidentSummary> currentResidents = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long unitId = rs.getLong("id");

                    // Si es una nueva unidad, guardar la anterior
                    if (currentUnit != null && !currentUnit.id().equals(unitId)) {
                        result.add(new HousingUnitWithResidents(currentUnit, new ArrayList<>(currentResidents)));
                        currentResidents.clear();
                    }

                    // Si es una nueva unidad, crear el objeto
                    if (currentUnit == null || !currentUnit.id().equals(unitId)) {
                        currentUnit = new HousingUnitResponse(
                                unitId,
                                rs.getLong("building_id"),
                                rs.getString("number"),
                                rs.getString("tower"),
                                rs.getString("floor"),
                                rs.getBigDecimal("aliquot_percentage"),
                                rs.getBigDecimal("square_meters"),
                                rs.getString("status"),
                                (Long) rs.getObject("created_by_user_id"),
                                rs.getString("creator_name"),
                                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime()
                                        : null,
                                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime()
                                        : null,
                                0 // Se calculará después
                        );
                    }

                    // Agregar residente si existe
                    Long residentId = (Long) rs.getObject("resident_id");
                    if (residentId != null) {
                        currentResidents.add(new HousingUnitWithResidents.ResidentSummary(
                                residentId,
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("email"),
                                rs.getString("phone"),
                                rs.getString("document_number")));
                    }
                }

                // Agregar la última unidad
                if (currentUnit != null) {
                    // Actualizar el conteo de residentes
                    currentUnit = new HousingUnitResponse(
                            currentUnit.id(),
                            currentUnit.buildingId(),
                            currentUnit.number(),
                            currentUnit.tower(),
                            currentUnit.floor(),
                            currentUnit.aliquotPercentage(),
                            currentUnit.squareMeters(),
                            currentUnit.status(),
                            currentUnit.createdByUserId(),
                            currentUnit.createdByUserName(),
                            currentUnit.createdAt(),
                            currentUnit.updatedAt(),
                            currentResidents.size());
                    result.add(new HousingUnitWithResidents(currentUnit, currentResidents));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching housing units with residents", e);
        }
    }

    /**
     * Obtener información completa de una unidad con residentes
     */
    public Optional<HousingUnitResponse> findByIdWithDetails(Long id) {
        boolean hasAuditColumns = checkAuditColumnsExist();
        String sql = hasAuditColumns ? """
                SELECT hu.id, hu.building_id, hu.number, hu.tower, hu.floor,
                       hu.aliquot_percentage, hu.square_meters, hu.status,
                       hu.created_by_user_id, hu.created_at, hu.updated_at,
                       CONCAT(creator.first_name, ' ', creator.last_name) as creator_name,
                       COUNT(u.id) as resident_count
                FROM housing_units hu
                LEFT JOIN users creator ON creator.id = hu.created_by_user_id
                LEFT JOIN users u ON u.unit_id = hu.id AND u.status = 'ACTIVE'
                WHERE hu.id = ?
                GROUP BY hu.id, hu.building_id, hu.number, hu.tower, hu.floor,
                         hu.aliquot_percentage, hu.square_meters, hu.status,
                         hu.created_by_user_id, hu.created_at, hu.updated_at,
                         creator.first_name, creator.last_name
                """ : """
                SELECT hu.id, hu.building_id, hu.number, hu.tower, hu.floor,
                       hu.aliquot_percentage, hu.square_meters, hu.status,
                       NULL as created_by_user_id, NULL as created_at, NULL as updated_at,
                       NULL as creator_name,
                       COUNT(u.id) as resident_count
                FROM housing_units hu
                LEFT JOIN users u ON u.unit_id = hu.id AND u.status = 'ACTIVE'
                WHERE hu.id = ?
                GROUP BY hu.id, hu.building_id, hu.number, hu.tower, hu.floor,
                         hu.aliquot_percentage, hu.square_meters, hu.status
                """;

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new HousingUnitResponse(
                            rs.getLong("id"),
                            rs.getLong("building_id"),
                            rs.getString("number"),
                            rs.getString("tower"),
                            rs.getString("floor"),
                            rs.getBigDecimal("aliquot_percentage"),
                            rs.getBigDecimal("square_meters"),
                            rs.getString("status"),
                            (Long) rs.getObject("created_by_user_id"),
                            rs.getString("creator_name"),
                            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime()
                                    : null,
                            rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime()
                                    : null,
                            rs.getInt("resident_count")));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching housing unit with details", e);
        }
        return Optional.empty();
    }

    private HousingUnit mapRow(ResultSet rs) throws SQLException {
        return new HousingUnit(
                rs.getLong("id"),
                rs.getLong("building_id"),
                rs.getString("number"),
                rs.getString("tower"),
                rs.getString("floor"),
                rs.getBigDecimal("aliquot_percentage"),
                rs.getBigDecimal("square_meters"),
                rs.getString("status"),
                (Long) rs.getObject("created_by_user_id"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
    }

    /**
     * Verificar si las columnas de auditoría existen en la tabla housing_units
     * El resultado se cachea para evitar múltiples consultas
     */
    private boolean checkAuditColumnsExist() {
        if (auditColumnsExist != null) {
            return auditColumnsExist;
        }
        String sql = """
                SELECT COUNT(*) as count
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                AND TABLE_NAME = 'housing_units'
                AND COLUMN_NAME IN ('created_by_user_id', 'created_at', 'updated_at')
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    auditColumnsExist = rs.getInt("count") == 3; // Las 3 columnas deben existir
                    return auditColumnsExist;
                }
            }
        } catch (SQLException e) {
            // Si hay error al verificar, asumimos que no existen (modo seguro)
            auditColumnsExist = false;
            return false;
        }
        auditColumnsExist = false;
        return false;
    }
}
