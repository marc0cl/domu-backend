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

public class VisitContactRepository {

    private final DataSource dataSource;

    @Inject
    public VisitContactRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ContactRow insert(ContactRow contact) {
        String sql = """
                INSERT INTO visit_contacts (resident_user_id, visitor_name, visitor_document, unit_id, alias, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime now = contact.createdAt() != null ? contact.createdAt() : LocalDateTime.now();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, contact.residentUserId());
            statement.setString(2, contact.visitorName());
            if (contact.visitorDocument() != null && !contact.visitorDocument().isBlank()) {
                statement.setString(3, contact.visitorDocument());
            } else {
                statement.setNull(3, java.sql.Types.VARCHAR);
            }
            if (contact.unitId() != null) {
                statement.setLong(4, contact.unitId());
            } else {
                statement.setNull(4, java.sql.Types.BIGINT);
            }
            if (contact.alias() != null && !contact.alias().isBlank()) {
                statement.setString(5, contact.alias());
            } else {
                statement.setNull(5, java.sql.Types.VARCHAR);
            }
            statement.setTimestamp(6, Timestamp.valueOf(now));
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Long id = keys.getLong(1);
                    return new ContactRow(
                            id,
                            contact.residentUserId(),
                            contact.visitorName(),
                            contact.visitorDocument(),
                            contact.unitId(),
                            contact.alias(),
                            now,
                            now
                    );
                }
            }
            throw new RepositoryException("No se pudo obtener el id del contacto");
        } catch (SQLException e) {
            throw new RepositoryException("Error guardando contacto de visita", e);
        }
    }

    private static final Integer DEFAULT_LIMIT = 20;

    public List<ContactRow> list(Long residentUserId, String search, Integer limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, resident_user_id, visitor_name, visitor_document, unit_id, alias, created_at, updated_at
                FROM visit_contacts
                WHERE resident_user_id = ?
                """);
        boolean hasSearch = search != null && !search.isBlank();
        if (hasSearch) {
            sql.append(" AND (LOWER(visitor_name) LIKE ? OR REPLACE(REPLACE(REPLACE(LOWER(COALESCE(visitor_document, '')), '.', ''), '-', ''), ' ', '') LIKE ?) ");
        }
        sql.append(" ORDER BY updated_at DESC, created_at DESC LIMIT ?");

        List<ContactRow> contacts = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setLong(1, residentUserId);
            Integer paramIndex = 2;
            if (hasSearch) {
                String normalizedSearch = search.toLowerCase().trim();
                String normalizedDoc = normalizedSearch.replace(".", "").replace("-", "").replace(" ", "");
                statement.setString(paramIndex, "%" + normalizedSearch + "%");
                paramIndex++;
                statement.setString(paramIndex, "%" + normalizedDoc + "%");
                paramIndex++;
            }
            Integer appliedLimit = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;
            statement.setInt(paramIndex, appliedLimit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    contacts.add(map(rs));
                }
            }
            return contacts;
        } catch (SQLException e) {
            throw new RepositoryException("Error obteniendo contactos de visita", e);
        }
    }

    public Optional<ContactRow> findById(Long contactId, Long residentUserId) {
        String sql = """
                SELECT id, resident_user_id, visitor_name, visitor_document, unit_id, alias, created_at, updated_at
                FROM visit_contacts
                WHERE id = ? AND resident_user_id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contactId);
            statement.setLong(2, residentUserId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error buscando contacto de visita", e);
        }
    }

    public void delete(Long contactId, Long residentUserId) {
        String sql = "DELETE FROM visit_contacts WHERE id = ? AND resident_user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contactId);
            statement.setLong(2, residentUserId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error eliminando contacto de visita", e);
        }
    }

    private ContactRow map(ResultSet rs) throws SQLException {
        return new ContactRow(
                rs.getLong("id"),
                rs.getLong("resident_user_id"),
                rs.getString("visitor_name"),
                rs.getString("visitor_document"),
                rs.getObject("unit_id") != null ? rs.getLong("unit_id") : null,
                rs.getString("alias"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    public record ContactRow(
            Long id,
            Long residentUserId,
            String visitorName,
            String visitorDocument,
            Long unitId,
            String alias,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}

