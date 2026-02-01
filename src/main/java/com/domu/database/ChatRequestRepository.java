package com.domu.database;

import com.domu.dto.ChatRequestResponse;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChatRequestRepository {

    private final DataSource dataSource;

    @Inject
    public ChatRequestRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Long insertRequest(Long senderId, Long receiverId, Long buildingId, Long itemId, String message) {
        String sql = """
                INSERT INTO chat_request (sender_id, receiver_id, building_id, item_id, initial_message)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, senderId);
            stmt.setLong(2, receiverId);
            stmt.setLong(3, buildingId);
            if (itemId != null) stmt.setLong(4, itemId); else stmt.setNull(4, Types.BIGINT);
            stmt.setString(5, message);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new RepositoryException("Error obteniendo ID de la solicitud de chat");
        } catch (SQLException e) {
            throw new RepositoryException("Error creando solicitud de chat", e);
        }
    }

    public List<ChatRequestResponse> findPendingByReceiver(Long receiverId) {
        String sql = """
                SELECT cr.*, u.firstName, u.lastName, mi.title as item_title
                FROM chat_request cr
                JOIN users u ON cr.sender_id = u.id
                LEFT JOIN market_item mi ON cr.item_id = mi.id
                WHERE cr.receiver_id = ? AND cr.status = 'PENDING'
                ORDER BY cr.created_at DESC
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, receiverId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<ChatRequestResponse> requests = new ArrayList<>();
                while (rs.next()) {
                    requests.add(mapResponse(rs));
                }
                return requests;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error buscando solicitudes de chat", e);
        }
    }

    public void updateStatus(Long requestId, String status) {
        String sql = "UPDATE chat_request SET status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setLong(2, requestId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error actualizando estado de solicitud de chat", e);
        }
    }

    public Optional<ChatRequestResponse> findById(Long id) {
        String sql = """
                SELECT cr.*, u.firstName, u.lastName, mi.title as item_title
                FROM chat_request cr
                JOIN users u ON cr.sender_id = u.id
                LEFT JOIN market_item mi ON cr.item_id = mi.id
                WHERE cr.id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResponse(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException("Error buscando solicitud de chat por ID", e);
        }
    }

    private ChatRequestResponse mapResponse(ResultSet rs) throws SQLException {
        return ChatRequestResponse.builder()
                .id(rs.getLong("id"))
                .senderId(rs.getLong("sender_id"))
                .senderName(rs.getString("firstName") + " " + rs.getString("lastName"))
                .receiverId(rs.getLong("receiver_id"))
                .buildingId(rs.getLong("building_id"))
                .itemId(rs.getObject("item_id") != null ? rs.getLong("item_id") : null)
                .itemTitle(rs.getString("item_title"))
                .status(rs.getString("status"))
                .initialMessage(rs.getString("initial_message"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }
}
