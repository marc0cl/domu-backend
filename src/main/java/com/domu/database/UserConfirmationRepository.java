package com.domu.database;

import com.google.inject.Inject;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserConfirmationRepository {
    private final DataSource dataSource;

    @Inject
    public UserConfirmationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insert(Long userId, String token, LocalDateTime expiresAt) {
        String sql = "INSERT INTO user_confirmations (user_id, token, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, token);
            stmt.setTimestamp(3, Timestamp.valueOf(expiresAt));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error saving user confirmation", e);
        }
    }

    public Optional<ConfirmationRow> findByToken(String token) {
        String sql = "SELECT id, user_id, token, expires_at, confirmed_at FROM user_confirmations WHERE token = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ConfirmationRow(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            rs.getString("token"),
                            rs.getTimestamp("expires_at").toLocalDateTime(),
                            rs.getTimestamp("confirmed_at") != null ? rs.getTimestamp("confirmed_at").toLocalDateTime() : null
                    ));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error finding confirmation by token", e);
        }
    }

    public void markAsConfirmed(String token) {
        String sql = "UPDATE user_confirmations SET confirmed_at = NOW() WHERE token = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error marking confirmation as used", e);
        }
    }

    public Optional<String> findLatestTokenForUser(Long userId) {
        String sql = "SELECT token FROM user_confirmations WHERE user_id = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("token"));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error finding latest token", e);
        }
    }

    public record ConfirmationRow(Long id, Long userId, String token, LocalDateTime expiresAt, LocalDateTime confirmedAt) {}
}
