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
        // Verificar que el usuario existe primero
        Optional<User> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new RepositoryException("Usuario no encontrado con id: " + userId);
        }

        String sql = "UPDATE users SET unit_id = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (unitId != null) {
                statement.setLong(1, unitId);
            } else {
                statement.setNull(1, Types.BIGINT);
            }
            statement.setLong(2, userId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException(
                        "No se pudo actualizar el usuario. El usuario podría no existir o no haber cambios.");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error updating user unit_id: " + e.getMessage(), e);
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, unit_id, role_id, first_name, last_name, birth_date, email, phone, password_hash, document_number, resident, created_at, status FROM users WHERE email = ?";
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
        String sql = "SELECT id, unit_id, role_id, first_name, last_name, birth_date, email, phone, password_hash, document_number, resident, created_at, status FROM users WHERE id = ?";
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
        String sql = "INSERT INTO users (unit_id, role_id, first_name, last_name, birth_date, email, phone, password_hash, document_number, resident, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (user.unitId() != null) {
                statement.setLong(1, user.unitId());
            } else {
                statement.setNull(1, Types.BIGINT);
            }
            if (user.roleId() != null) {
                statement.setLong(2, user.roleId());
            } else {
                statement.setNull(2, Types.BIGINT);
            }
            statement.setString(3, user.firstName());
            statement.setString(4, user.lastName());
            if (user.birthDate() != null) {
                statement.setDate(5, Date.valueOf(user.birthDate()));
            } else {
                statement.setNull(5, Types.DATE);
            }
            statement.setString(6, user.email());
            statement.setString(7, user.phone());
            statement.setString(8, user.passwordHash());
            statement.setString(9, user.documentNumber());
            statement.setBoolean(10, user.resident());
            if (user.status() != null) {
                statement.setString(11, user.status());
            } else {
                statement.setString(11, "ACTIVE");
            }

            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    return user.withId(id);
                }
            }
            throw new RepositoryException("User insert did not return a generated identifier");
        } catch (SQLException e) {
            throw new RepositoryException("Error saving user", e);
        }
    }

    /**
     * Encuentra todos los usuarios de un edificio, incluyendo información de su
     * unidad.
     * Incluye usuarios vinculados al edificio vía user_buildings, incluso si no
     * tienen unidad asignada.
     * Ordena por número de unidad y luego por nombre.
     */
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

    public record ResidentWithUnit(
            Long id,
            Long unitId,
            Long roleId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String documentNumber,
            Boolean resident,
            java.time.LocalDateTime createdAt,
            String status,
            String unitNumber,
            String tower,
            String floor) {
    }

    private User mapRow(ResultSet resultSet) throws SQLException {
        Long id = resultSet.getLong("id");
        Object unitObject = resultSet.getObject("unit_id");
        Long unitId = unitObject != null ? resultSet.getLong("unit_id") : null;
        Object roleObject = resultSet.getObject("role_id");
        Long roleId = roleObject != null ? resultSet.getLong("role_id") : null;
        String firstName = resultSet.getString("first_name");
        String lastName = resultSet.getString("last_name");
        Date birthDateRaw = resultSet.getDate("birth_date");
        LocalDate birthDate = birthDateRaw != null ? birthDateRaw.toLocalDate() : null;
        String email = resultSet.getString("email");
        String phone = resultSet.getString("phone");
        String passwordHash = resultSet.getString("password_hash");
        String documentNumber = resultSet.getString("document_number");
        Boolean resident = (Boolean) resultSet.getObject("resident");
        java.sql.Timestamp createdAtRaw = resultSet.getTimestamp("created_at");
        java.time.LocalDateTime createdAt = createdAtRaw != null ? createdAtRaw.toLocalDateTime() : null;
        String status = resultSet.getString("status");
        return new User(
                id,
                unitId,
                roleId,
                firstName,
                lastName,
                email,
                phone,
                birthDate,
                passwordHash,
                documentNumber,
                resident,
                createdAt,
                status);
    }
}
