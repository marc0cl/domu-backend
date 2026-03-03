package com.domu.database;

import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationRepository {

    private final DataSource dataSource;

    @Inject
    public NotificationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public record NotificationRow(
        Long id,
        Long buildingId,
        Long userId,
        String type,
        String title,
        String message,
        String data,
        boolean isRead,
        LocalDateTime createdAt,
        LocalDateTime readAt
    ) {}

    public record PreferenceRow(
        Long id,
        Long userId,
        String notificationType,
        boolean inAppEnabled
    ) {}

    public NotificationRow insert(NotificationRow row) {
        String sql = "INSERT INTO notifications (building_id, user_id, type, title, message, data, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?, FALSE, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, row.buildingId());
            stmt.setLong(2, row.userId());
            stmt.setString(3, row.type());
            stmt.setString(4, row.title());
            stmt.setString(5, row.message());
            stmt.setString(6, row.data());
            stmt.setTimestamp(7, Timestamp.valueOf(row.createdAt() != null ? row.createdAt() : LocalDateTime.now()));
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new NotificationRow(keys.getLong(1), row.buildingId(), row.userId(),
                            row.type(), row.title(), row.message(), row.data(), false,
                            row.createdAt() != null ? row.createdAt() : LocalDateTime.now(), null);
                }
            }
            throw new RepositoryException("Failed to get generated key for notification insert");
        } catch (SQLException e) {
            throw new RepositoryException("Error inserting notification", e);
        }
    }

    public void insertBatch(List<NotificationRow> rows) {
        if (rows == null || rows.isEmpty()) return;
        String sql = "INSERT INTO notifications (building_id, user_id, type, title, message, data, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?, FALSE, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            for (NotificationRow row : rows) {
                stmt.setLong(1, row.buildingId());
                stmt.setLong(2, row.userId());
                stmt.setString(3, row.type());
                stmt.setString(4, row.title());
                stmt.setString(5, row.message());
                stmt.setString(6, row.data());
                stmt.setTimestamp(7, Timestamp.valueOf(row.createdAt() != null ? row.createdAt() : now));
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RepositoryException("Error batch inserting notifications", e);
        }
    }

    public List<NotificationRow> findByUserAndBuilding(Long userId, Long buildingId, int limit, int offset) {
        String sql = "SELECT id, building_id, user_id, type, title, message, data, is_read, created_at, read_at FROM notifications WHERE user_id = ? AND building_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<NotificationRow> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, buildingId);
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding notifications", e);
        }
        return results;
    }

    public int countUnread(Long userId, Long buildingId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND building_id = ? AND is_read = FALSE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, buildingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error counting unread notifications", e);
        }
        return 0;
    }

    public boolean markRead(Long id, Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE, read_at = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, id);
            stmt.setLong(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Error marking notification as read", e);
        }
    }

    public int markAllRead(Long userId, Long buildingId) {
        String sql = "UPDATE notifications SET is_read = TRUE, read_at = ? WHERE user_id = ? AND building_id = ? AND is_read = FALSE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, userId);
            stmt.setLong(3, buildingId);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error marking all notifications as read", e);
        }
    }

    public List<PreferenceRow> findPreferences(Long userId) {
        String sql = "SELECT id, user_id, notification_type, in_app_enabled FROM notification_preferences WHERE user_id = ?";
        List<PreferenceRow> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new PreferenceRow(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("notification_type"),
                        rs.getBoolean("in_app_enabled")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding notification preferences", e);
        }
        return results;
    }

    public void upsertPreference(Long userId, String notificationType, boolean inAppEnabled) {
        String sql = "INSERT INTO notification_preferences (user_id, notification_type, in_app_enabled) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE in_app_enabled = VALUES(in_app_enabled)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, notificationType);
            stmt.setBoolean(3, inAppEnabled);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error upserting notification preference", e);
        }
    }

    private NotificationRow mapRow(ResultSet rs) throws SQLException {
        Timestamp readAtTs = rs.getTimestamp("read_at");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        return new NotificationRow(
            rs.getLong("id"),
            rs.getLong("building_id"),
            rs.getLong("user_id"),
            rs.getString("type"),
            rs.getString("title"),
            rs.getString("message"),
            rs.getString("data"),
            rs.getBoolean("is_read"),
            createdAtTs != null ? createdAtTs.toLocalDateTime() : null,
            readAtTs != null ? readAtTs.toLocalDateTime() : null
        );
    }
}
