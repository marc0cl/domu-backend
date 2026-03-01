package com.domu.database;

import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class PasswordResetRepository {

    private final DataSource dataSource;

    @Inject
    public PasswordResetRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insert(Long userId, String token, LocalDateTime expiresAt) {
        String sql = "INSERT INTO password_reset_tokens (user_id, token, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, token);
            stmt.setTimestamp(3, Timestamp.valueOf(expiresAt));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error saving password reset token", e);
        }
    }

    public Optional<ResetRow> findByToken(String token) {
        String sql = "SELECT id, user_id, token, expires_at, used_at FROM password_reset_tokens WHERE token = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ResetRow(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            rs.getString("token"),
                            rs.getTimestamp("expires_at").toLocalDateTime(),
                            rs.getTimestamp("used_at") != null ? rs.getTimestamp("used_at").toLocalDateTime() : null
                    ));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error finding password reset token", e);
        }
    }

    public void markAsUsed(String token) {
        String sql = "UPDATE password_reset_tokens SET used_at = NOW() WHERE token = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error marking password reset token as used", e);
        }
    }

    public record ResetRow(Long id, Long userId, String token, LocalDateTime expiresAt, LocalDateTime usedAt) {}
}
