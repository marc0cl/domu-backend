package com.domu.backend.infrastructure.persistence;

import com.domu.backend.domain.core.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserRepository {

    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id_usuario, id_unidad, id_rol, nombres, apellidos, fecha_nacimiento, correo, telefono, password_hash, documento, es_residente, fecha_creacion, estado FROM usuario WHERE correo = ?";
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
        String sql = "SELECT id_usuario, id_unidad, id_rol, nombres, apellidos, fecha_nacimiento, correo, telefono, password_hash, documento, es_residente, fecha_creacion, estado FROM usuario WHERE id_usuario = ?";
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
        String sql = "INSERT INTO usuario (id_unidad, id_rol, nombres, apellidos, fecha_nacimiento, correo, telefono, password_hash, documento, es_residente, fecha_creacion, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            if (user.phone() != null) {
                statement.setString(7, user.phone());
            } else {
                statement.setNull(7, Types.VARCHAR);
            }
            statement.setString(8, user.passwordHash());
            if (user.documentNumber() != null) {
                statement.setString(9, user.documentNumber());
            } else {
                statement.setNull(9, Types.VARCHAR);
            }
            if (user.resident() != null) {
                statement.setBoolean(10, user.resident());
            } else {
                statement.setNull(10, Types.BOOLEAN);
            }
            if (user.createdAt() != null) {
                statement.setTimestamp(11, Timestamp.valueOf(user.createdAt()));
            } else {
                statement.setNull(11, Types.TIMESTAMP);
            }
            if (user.status() != null) {
                statement.setString(12, user.status());
            } else {
                statement.setNull(12, Types.VARCHAR);
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
        Long id = resultSet.getLong("id_usuario");
        Object unitObject = resultSet.getObject("id_unidad");
        Long unitId = unitObject != null ? resultSet.getLong("id_unidad") : null;
        Object roleObject = resultSet.getObject("id_rol");
        Long roleId = roleObject != null ? resultSet.getLong("id_rol") : null;
        String firstName = resultSet.getString("nombres");
        String lastName = resultSet.getString("apellidos");
        Date birthDateRaw = resultSet.getDate("fecha_nacimiento");
        LocalDate birthDate = birthDateRaw != null ? birthDateRaw.toLocalDate() : null;
        String email = resultSet.getString("correo");
        String phone = resultSet.getString("telefono");
        String passwordHash = resultSet.getString("password_hash");
        String documentNumber = resultSet.getString("documento");
        Object residentObject = resultSet.getObject("es_residente");
        Boolean resident = residentObject != null ? resultSet.getBoolean("es_residente") : null;
        Timestamp createdRaw = resultSet.getTimestamp("fecha_creacion");
        LocalDateTime createdAt = createdRaw != null ? createdRaw.toLocalDateTime() : null;
        String status = resultSet.getString("estado");
        return new User(id, unitId, roleId, firstName, lastName, email, phone, birthDate, passwordHash, documentNumber, resident, createdAt, status);
    }
}
