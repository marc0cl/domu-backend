package com.domu.database;

import com.domu.dto.BuildingSummaryResponse;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserBuildingRepository {

    private final DataSource dataSource;

    @Inject
    public UserBuildingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<BuildingSummaryResponse> findBuildingsForUser(Long userId) {
        String sql = """
                SELECT b.id, b.name, b.address, b.city, b.commune
                FROM user_buildings ub
                JOIN buildings b ON b.id = ub.building_id
                WHERE ub.user_id = ?
                ORDER BY b.name
                """;
        List<BuildingSummaryResponse> buildings = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    buildings.add(new BuildingSummaryResponse(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("address"),
                            rs.getString("commune"),
                            rs.getString("city")));
                }
            }
            return buildings;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo edificios del usuario", e);
        }
    }

    public void addUserToBuilding(Long userId, Long buildingId) {
        String sql = """
                INSERT IGNORE INTO user_buildings (user_id, building_id)
                VALUES (?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, buildingId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error vinculando usuario a edificio", e);
        }
    }

    /**
     * Verifica si un usuario tiene acceso a un edificio específico.
     */
    public boolean userHasAccessToBuilding(Long userId, Long buildingId) {
        if (userId == null || buildingId == null) {
            return false;
        }
        String sql = """
                SELECT 1 FROM user_buildings
                WHERE user_id = ? AND building_id = ?
                LIMIT 1
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error verificando acceso a edificio", e);
        }
    }

    public List<Long> findUserIdsByBuilding(Long buildingId) {
        String sql = "SELECT user_id FROM user_buildings WHERE building_id = ?";
        List<Long> userIds = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getLong("user_id"));
                }
            }
            return userIds;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo usuarios del edificio", e);
        }
    }

    public List<Long> findAdminUserIdsByBuilding(Long buildingId) {
        String sql = """
                SELECT ub.user_id FROM user_buildings ub
                JOIN users u ON u.id = ub.user_id
                WHERE ub.building_id = ? AND u.role_id = 1
                """;
        List<Long> userIds = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getLong("user_id"));
                }
            }
            return userIds;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo admins del edificio", e);
        }
    }

    public List<Long> findUserIdsByUnitId(Long unitId) {
        String sql = "SELECT id FROM users WHERE unit_id = ? AND status = 'ACTIVE'";
        List<Long> userIds = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, unitId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getLong("id"));
                }
            }
            return userIds;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo usuarios de la unidad", e);
        }
    }

    public List<Long> findUserIdsByBuildingAndRoles(Long buildingId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT ub.user_id FROM user_buildings ub JOIN users u ON u.id = ub.user_id WHERE ub.building_id = ? AND u.role_id IN (");
        for (int i = 0; i < roleIds.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
        }
        sql.append(")");
        List<Long> userIds = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setLong(1, buildingId);
            for (int i = 0; i < roleIds.size(); i++) {
                statement.setLong(i + 2, roleIds.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getLong("user_id"));
                }
            }
            return userIds;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo usuarios por roles del edificio", e);
        }
    }
}
