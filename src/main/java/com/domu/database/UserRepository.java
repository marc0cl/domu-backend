package com.domu.database;

import com.domu.domain.core.User;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    private final DataSource dataSource;

    @Inject
    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public User updateProfile(Long id, String firstName, String lastName, String phone, String documentNumber) {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, phone = ?, document_number = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setString(3, phone);
            statement.setString(4, documentNumber);
            statement.setLong(5, id);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No user updated");
            }
            return findById(id).orElseThrow(() -> new RepositoryException("User not found after update"));
        } catch (SQLException e) {
            throw new RepositoryException("Error updating user profile", e);
        }
    }

    public void updatePassword(Long id, String hash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hash);
            statement.setLong(2, id);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No user updated");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error updating user password", e);
        }
    }

    public void setStatus(Long id, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error updating user status", e);
        }
    }

    public void updateUnitId(Long userId, Long unitId) {
        String sql = "UPDATE users SET unit_id = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (unitId != null) {
                statement.setLong(1, unitId);
            } else {
                statement.setNull(1, Types.BIGINT);
            }
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error updating user unit_id", e);
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, unit_id, role_id, first_name, last_name, birth_date, email, phone, password_hash, document_number, resident, created_at, status, bio, avatar_box_id, privacy_avatar_box_id FROM users WHERE email = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching user by email", e);
        }
        return Optional.empty();
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT id, unit_id, role_id, first_name, last_name, birth_date, email, phone, password_hash, document_number, resident, created_at, status, bio, avatar_box_id, privacy_avatar_box_id FROM users WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching user by id", e);
        }
        return Optional.empty();
    }

    public User save(User user) {
        String sql = "INSERT INTO users (unit_id, role_id, first_name, last_name, birth_date, email, phone, password_hash, document_number, resident, status, bio, avatar_box_id, privacy_avatar_box_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (user.unitId() != null) statement.setLong(1, user.unitId()); else statement.setNull(1, Types.BIGINT);
            if (user.roleId() != null) statement.setLong(2, user.roleId()); else statement.setNull(2, Types.BIGINT);
            statement.setString(3, user.firstName());
            statement.setString(4, user.lastName());
            if (user.birthDate() != null) statement.setDate(5, Date.valueOf(user.birthDate())); else statement.setNull(5, Types.DATE);
            statement.setString(6, user.email());
            statement.setString(7, user.phone());
            statement.setString(8, user.passwordHash());
            statement.setString(9, user.documentNumber());
            statement.setBoolean(10, user.resident());
            statement.setString(11, user.status() != null ? user.status() : "ACTIVE");
            statement.setString(12, user.bio());
            statement.setString(13, user.avatarBoxId());
            statement.setString(14, user.privacyAvatarBoxId());

            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) return user.withId(generatedKeys.getLong(1));
            }
            throw new RepositoryException("User insert did not return an ID");
        } catch (SQLException e) {
            throw new RepositoryException("Error saving user", e);
        }
    }

    public void updateAvatar(Long userId, String avatarBoxId) {
        String sql = "UPDATE users SET avatar_box_id = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, avatarBoxId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error updating user avatar", e);
        }
    }

    public void updatePrivacyAvatar(Long userId, String privacyAvatarBoxId) {
        String sql = "UPDATE users SET privacy_avatar_box_id = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, privacyAvatarBoxId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error updating user privacy avatar", e);
        }
    }

    public List<ChatNeighborSummary> findNeighborsForChat(Long buildingId, Long currentUserId) {
        String sql = """
                SELECT u.id, hu.number AS unit_number, u.privacy_avatar_box_id
                FROM users u
                INNER JOIN user_buildings ub ON ub.user_id = u.id AND ub.building_id = ?
                INNER JOIN housing_units hu ON hu.id = u.unit_id
                WHERE u.id != ? AND u.status = 'ACTIVE'
                ORDER BY hu.number ASC
                """;
        List<ChatNeighborSummary> neighbors = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            statement.setLong(2, currentUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    neighbors.add(new ChatNeighborSummary(
                            rs.getLong("id"),
                            rs.getString("unit_number"),
                            rs.getString("privacy_avatar_box_id")
                    ));
                }
            }
            return neighbors;
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching neighbors for chat", e);
        }
    }

    public record ChatNeighborSummary(Long id, String unitNumber, String privacyAvatarBoxId) {}

    public List<ResidentWithUnit> findResidentsByBuilding(Long buildingId) {
        String sql = """
                SELECT DISTINCT u.id, u.unit_id, u.role_id, u.first_name, u.last_name, u.email, u.phone,
                       u.document_number, u.resident, u.created_at, u.status,
                       hu.number AS unit_number, hu.tower, hu.floor
                FROM users u
                INNER JOIN user_buildings ub ON ub.user_id = u.id AND ub.building_id = ?
                LEFT JOIN housing_units hu ON hu.id = u.unit_id AND hu.building_id = ?
                WHERE u.status = 'ACTIVE'
                ORDER BY hu.floor, hu.number, u.first_name, u.last_name
                """;
        List<ResidentWithUnit> residents = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            statement.setLong(2, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    residents.add(mapResidentWithUnit(rs));
                }
            }
            return residents;
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching residents by building", e);
        }
    }

    private ResidentWithUnit mapResidentWithUnit(ResultSet rs) throws SQLException {
        return new ResidentWithUnit(
                rs.getLong("id"),
                (Long) rs.getObject("unit_id"),
                (Long) rs.getObject("role_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("document_number"),
                rs.getBoolean("resident"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getString("status"),
                rs.getString("unit_number"),
                rs.getString("tower"),
                rs.getString("floor"));
    }

    public record ResidentWithUnit(Long id, Long unitId, Long roleId, String firstName, String lastName, String email, String phone, String documentNumber, Boolean resident, java.time.LocalDateTime createdAt, String status, String unitNumber, String tower, String floor) {}

    private User mapRow(ResultSet rs) throws SQLException {
        Date birthDateRaw = rs.getDate("birth_date");
        java.sql.Timestamp createdAtRaw = rs.getTimestamp("created_at");
        
        return new User(
                rs.getLong("id"),
                (Long) rs.getObject("unit_id"),
                (Long) rs.getObject("role_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                birthDateRaw != null ? birthDateRaw.toLocalDate() : null,
                rs.getString("password_hash"),
                rs.getString("document_number"),
                rs.getBoolean("resident"),
                createdAtRaw != null ? createdAtRaw.toLocalDateTime() : null,
                rs.getString("status"),
                rs.getString("bio"),
                rs.getString("avatar_box_id"),
                rs.getString("privacy_avatar_box_id"));
    }
}