package com.domu.database;

import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PollRepository {

    private final DataSource dataSource;

    @Inject
    public PollRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public PollRow insertPoll(PollRow poll) {
        String sql = """
                INSERT INTO polls (building_id, created_by, title, description, closes_at, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime createdAt = poll.createdAt() != null ? poll.createdAt() : LocalDateTime.now();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, poll.buildingId());
            statement.setLong(2, poll.createdBy());
            statement.setString(3, poll.title());
            statement.setString(4, poll.description());
            statement.setTimestamp(5, Timestamp.valueOf(poll.closesAt()));
            statement.setString(6, poll.status());
            statement.setTimestamp(7, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return new PollRow(id, poll.buildingId(), poll.createdBy(), poll.title(), poll.description(),
                            poll.closesAt(), poll.status(), createdAt, null);
                }
            }
            throw new RepositoryException("No se pudo obtener el id de la votación");
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando la votación", e);
        }
    }

    public List<PollOptionRow> insertOptions(Long pollId, List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }
        String sql = """
                INSERT INTO poll_options (poll_id, label, votes)
                VALUES (?, ?, 0)
                """;
        List<PollOptionRow> saved = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (String label : labels) {
                statement.setLong(1, pollId);
                statement.setString(2, label);
                statement.addBatch();
            }
            statement.executeBatch();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                int idx = 0;
                while (keys.next() && idx < labels.size()) {
                    saved.add(new PollOptionRow(keys.getLong(1), pollId, labels.get(idx), 0));
                    idx++;
                }
            }
            return saved;
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando opciones de votación", e);
        }
    }

    public List<PollRow> findByBuilding(Long buildingId) {
        String sql = """
                SELECT id, building_id, created_by, title, description, closes_at, status, created_at, closed_at
                FROM polls
                WHERE building_id = ?
                ORDER BY created_at DESC
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            try (ResultSet rs = statement.executeQuery()) {
                List<PollRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapPoll(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo votaciones", e);
        }
    }

    public List<PollRow> findByBuildingAndStatus(Long buildingId, String status) {
        String sql = """
                SELECT id, building_id, created_by, title, description, closes_at, status, created_at, closed_at
                FROM polls
                WHERE building_id = ? AND status = ?
                ORDER BY created_at DESC
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buildingId);
            statement.setString(2, status);
            try (ResultSet rs = statement.executeQuery()) {
                List<PollRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapPoll(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo votaciones por estado", e);
        }
    }

    public List<PollRow> findExpiredOpenPolls(LocalDateTime limit) {
        String sql = """
                SELECT id, building_id, created_by, title, description, closes_at, status, created_at, closed_at
                FROM polls
                WHERE status = 'OPEN' AND closes_at <= ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(limit));
            try (ResultSet rs = statement.executeQuery()) {
                List<PollRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapPoll(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo votaciones vencidas", e);
        }
    }

    public Optional<PollRow> findById(Long id) {
        String sql = """
                SELECT id, building_id, created_by, title, description, closes_at, status, created_at, closed_at
                FROM polls
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPoll(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo la votación", e);
        }
    }

    public List<PollOptionRow> findOptions(Long pollId) {
        String sql = """
                SELECT id, poll_id, label, votes
                FROM poll_options
                WHERE poll_id = ?
                ORDER BY id ASC
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pollId);
            try (ResultSet rs = statement.executeQuery()) {
                List<PollOptionRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapOption(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo opciones de votación", e);
        }
    }

    public Optional<PollVoteRow> findUserVote(Long pollId, Long userId) {
        String sql = """
                SELECT id, poll_id, option_id, user_id, voted_at
                FROM poll_votes
                WHERE poll_id = ? AND user_id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pollId);
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapVote(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error verificando voto del usuario", e);
        }
    }

    public PollVoteRow insertVote(Long pollId, Long optionId, Long userId, LocalDateTime votedAt) {
        String insertVoteSql = """
                INSERT INTO poll_votes (poll_id, option_id, user_id, voted_at)
                VALUES (?, ?, ?, ?)
                """;
        String incrementOptionSql = """
                UPDATE poll_options
                SET votes = votes + 1
                WHERE id = ? AND poll_id = ?
                """;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement voteStmt = connection.prepareStatement(insertVoteSql, Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement incrementStmt = connection.prepareStatement(incrementOptionSql)) {
                voteStmt.setLong(1, pollId);
                voteStmt.setLong(2, optionId);
                voteStmt.setLong(3, userId);
                voteStmt.setTimestamp(4, Timestamp.valueOf(votedAt));
                voteStmt.executeUpdate();

                incrementStmt.setLong(1, optionId);
                incrementStmt.setLong(2, pollId);
                int updated = incrementStmt.executeUpdate();
                if (updated == 0) {
                    connection.rollback();
                    throw new RepositoryException("No se pudo incrementar el voto: opción no encontrada");
                }

                try (ResultSet keys = voteStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        Long id = keys.getLong(1);
                        connection.commit();
                        return new PollVoteRow(id, pollId, optionId, userId, votedAt);
                    }
                }
                connection.rollback();
                throw new RepositoryException("No se obtuvo el id del voto");
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error registrando voto", e);
        }
    }

    public PollRow closePoll(Long pollId, LocalDateTime closedAt) {
        String sql = """
                UPDATE polls
                SET status = 'CLOSED', closed_at = ?
                WHERE id = ?
                """;
        LocalDateTime effectiveClosedAt = closedAt != null ? closedAt : LocalDateTime.now();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(effectiveClosedAt));
            statement.setLong(2, pollId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new RepositoryException("No se encontró la votación para cerrar");
            }
            return findById(pollId).orElseThrow(() -> new RepositoryException("No se pudo recuperar la votación cerrada"));
        } catch (SQLException e) {
            throw new RepositoryException("Error cerrando la votación", e);
        }
    }

    private PollRow mapPoll(ResultSet rs) throws SQLException {
        Timestamp closedAtRaw = rs.getTimestamp("closed_at");
        return new PollRow(
                rs.getLong("id"),
                rs.getLong("building_id"),
                rs.getLong("created_by"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getTimestamp("closes_at").toLocalDateTime(),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                closedAtRaw != null ? closedAtRaw.toLocalDateTime() : null);
    }

    private PollOptionRow mapOption(ResultSet rs) throws SQLException {
        return new PollOptionRow(
                rs.getLong("id"),
                rs.getLong("poll_id"),
                rs.getString("label"),
                rs.getInt("votes"));
    }

    private PollVoteRow mapVote(ResultSet rs) throws SQLException {
        return new PollVoteRow(
                rs.getLong("id"),
                rs.getLong("poll_id"),
                rs.getLong("option_id"),
                rs.getLong("user_id"),
                rs.getTimestamp("voted_at").toLocalDateTime());
    }

    public record PollRow(
            Long id,
            Long buildingId,
            Long createdBy,
            String title,
            String description,
            LocalDateTime closesAt,
            String status,
            LocalDateTime createdAt,
            LocalDateTime closedAt) {
    }

    public record PollOptionRow(
            Long id,
            Long pollId,
            String label,
            Integer votes) {
    }

    public record PollVoteRow(
            Long id,
            Long pollId,
            Long optionId,
            Long userId,
            LocalDateTime votedAt) {
    }
}
