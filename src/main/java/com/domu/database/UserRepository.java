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
import java.util.Optional;

public class UserRepository {

    private final DataSource dataSource;

    @Inject
    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
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
            status
        );
    }
}
