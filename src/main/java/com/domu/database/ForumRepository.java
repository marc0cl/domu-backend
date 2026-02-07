package com.domu.database;

import com.domu.dto.ForumThreadDto;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ForumRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForumRepository.class);
    private final DataSource dataSource;
    private volatile boolean forumSchemaInitialized = false;

    @Inject
    public ForumRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<ForumThreadDto> findAllByBuildingId(Long buildingId) {
        ensureForumSchema();
        String sql = """
                    SELECT
                        t.id, t.title, t.created_at, t.pinned, t.author_id,
                        c.name as category_name, c.label as category_label, c.icon as category_icon,
                        u.first_name, u.last_name,
                        p.content
                    FROM forum_threads t
                    JOIN forum_categories c ON t.category_id = c.id
                    JOIN users u ON t.author_id = u.id
                    LEFT JOIN forum_posts p ON p.id = (
                        SELECT min(id) FROM forum_posts WHERE thread_id = t.id
                    )
                    WHERE t.building_id = ? AND t.status = 'ACTIVE'
                    ORDER BY t.pinned DESC, t.created_at DESC
                """;

        List<ForumThreadDto> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching forum threads", e);
        }
        return result;
    }

    public Optional<ForumThreadDto> findById(Long threadId) {
        ensureForumSchema();
        String sql = """
                    SELECT
                        t.id, t.title, t.created_at, t.pinned, t.author_id,
                        c.name as category_name, c.label as category_label, c.icon as category_icon,
                        u.first_name, u.last_name,
                        p.content
                    FROM forum_threads t
                    JOIN forum_categories c ON t.category_id = c.id
                    JOIN users u ON t.author_id = u.id
                    LEFT JOIN forum_posts p ON p.id = (
                        SELECT min(id) FROM forum_posts WHERE thread_id = t.id
                    )
                    WHERE t.id = ? AND t.status = 'ACTIVE'
                """;

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, threadId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching forum thread by id", e);
        }
        return Optional.empty();
    }

    public Long createThread(Long buildingId, Long authorId, Long categoryId, String title, boolean pinned) {
        ensureForumSchema();
        String sql = "INSERT INTO forum_threads (building_id, author_id, category_id, title, pinned) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, buildingId);
            statement.setLong(2, authorId);
            statement.setLong(3, categoryId);
            statement.setString(4, title);
            statement.setBoolean(5, pinned);

            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new RepositoryException("Creating thread failed, no ID obtained.");
        } catch (SQLException e) {
            throw new RepositoryException("Error creating forum thread", e);
        }
    }

    public void createPost(Long threadId, Long authorId, String content) {
        ensureForumSchema();
        String sql = "INSERT INTO forum_posts (thread_id, author_id, content) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, threadId);
            statement.setLong(2, authorId);
            statement.setString(3, content);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error creating forum post", e);
        }
    }

    public void updateThread(Long threadId, String title, Long categoryId, Boolean pinned) {
        ensureForumSchema();
        StringBuilder sql = new StringBuilder("UPDATE forum_threads SET ");
        List<Object> params = new ArrayList<>();

        if (title != null) {
            sql.append("title = ?, ");
            params.add(title);
        }
        if (categoryId != null) {
            sql.append("category_id = ?, ");
            params.add(categoryId);
        }
        if (pinned != null) {
            sql.append("pinned = ?, ");
            params.add(pinned);
        }

        if (params.isEmpty())
            return;

        // Remove trailing comma
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(threadId);

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error updating forum thread", e);
        }
    }

    public void updatePostContent(Long threadId, String content) {
        ensureForumSchema();
        // Update the first post of the thread
        String sql = "UPDATE forum_posts SET content = ? WHERE id = (SELECT id FROM (SELECT min(id) as id FROM forum_posts WHERE thread_id = ?) as temp)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, content);
            statement.setLong(2, threadId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error updating forum post", e);
        }
    }

    public void deleteThread(Long threadId) {
        ensureForumSchema();
        String sql = "UPDATE forum_threads SET status = 'DELETED' WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, threadId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error deleting forum thread", e);
        }
    }

    public Optional<Long> findCategoryIdByName(String name) {
        ensureForumSchema();
        String sql = "SELECT id FROM forum_categories WHERE name = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching category id", e);
        }
        return Optional.empty();
    }

    private ForumThreadDto mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        String authorName = rs.getString("first_name") + " " + rs.getString("last_name");
        return new ForumThreadDto(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("category_name"),
                rs.getString("category_label"),
                rs.getString("category_icon"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                rs.getLong("author_id"),
                authorName,
                rs.getBoolean("pinned"));
    }

    private void ensureForumSchema() {
        if (forumSchemaInitialized) {
            return;
        }
        synchronized (this) {
            if (forumSchemaInitialized) {
                return;
            }
            applyForumMigration();
            forumSchemaInitialized = true;
        }
    }

    private void applyForumMigration() {
        String migrationPath = "/migrations/023_forum.sql";
        try (InputStream inputStream = ForumRepository.class.getResourceAsStream(migrationPath)) {
            if (inputStream == null) {
                LOGGER.warn("Forum migration file not found at {}", migrationPath);
                return;
            }
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String[] statements = sql.split(";");
            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                for (String raw : statements) {
                    String trimmed = raw.trim();
                    if (!trimmed.isEmpty()) {
                        statement.execute(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            throw new RepositoryException("Error applying forum migration", e);
        }
    }
}