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
        String sql = "SELECT id_usuario, id_unidad, id_rol, nombres, apellidos, fecha_nacimiento, correo, password_hash FROM usuario WHERE correo = ?";
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
        String sql = "SELECT id_usuario, id_unidad, id_rol, nombres, apellidos, fecha_nacimiento, correo, password_hash FROM usuario WHERE id_usuario = ?";
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
        String sql = "INSERT INTO usuario (id_unidad, id_rol, nombres, apellidos, fecha_nacimiento, correo, password_hash) VALUES (?, ?, ?, ?, ?, ?, ?)";
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
            statement.setString(7, user.passwordHash());

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
        String passwordHash = resultSet.getString("password_hash");
        return new User(
            id,
            unitId,
            roleId,
            firstName,
            lastName,
            email,
            null,
            birthDate,
            passwordHash,
            null,
            null,
            null,
            null
        );
    }
}
