package com.domu.database;

import com.domu.dto.ChatMessageResponse;
import com.domu.dto.ChatRoomResponse;
import com.google.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChatRepository {

    private final DataSource dataSource;

    @Inject
    public ChatRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<ChatRoomResponse> findRoomsByUser(Long userId, Long buildingId) {
        String sql = """
                SELECT r.*, i.title as item_title, i.main_image_url as item_image
                FROM chat_room r
                JOIN chat_participant p ON r.id = p.room_id
                LEFT JOIN market_item i ON r.item_id = i.id
                WHERE p.user_id = ? AND r.building_id = ? AND p.hidden_at IS NULL
                ORDER BY r.last_message_at DESC
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, buildingId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<ChatRoomResponse> rooms = new ArrayList<>();
                while (rs.next()) {
                    Long roomId = rs.getLong("id");
                    rooms.add(ChatRoomResponse.builder()
                            .id(roomId)
                            .buildingId(rs.getLong("building_id"))
                            .itemId(rs.getLong("item_id"))
                            .itemTitle(rs.getString("item_title"))
                            .itemImageUrl(rs.getString("item_image"))
                            .participants(findParticipantsByRoom(roomId))
                            .lastMessage(findLastMessageByRoom(roomId).orElse(null))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .lastMessageAt(rs.getTimestamp("last_message_at").toLocalDateTime())
                            .build());
                }
                return rooms;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error buscando salas de chat", e);
        }
    }

    public Long createRoom(Long buildingId, Long itemId) {
        String sql = "INSERT INTO chat_room (building_id, item_id) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, buildingId);
            if (itemId != null) stmt.setLong(2, itemId); else stmt.setNull(2, Types.BIGINT);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new RepositoryException("Error obteniendo ID de la sala creada");
        } catch (SQLException e) {
            throw new RepositoryException("Error creando sala de chat", e);
        }
    }

    public void addParticipant(Long roomId, Long userId) {
        String sql = "INSERT IGNORE INTO chat_participant (room_id, user_id) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error añadiendo participante al chat", e);
        }
    }

    public Long insertMessage(Long roomId, Long senderId, String content, String type, String boxFileId) {
        String sql = "INSERT INTO chat_message (room_id, sender_id, content, type, box_file_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, senderId);
            stmt.setString(3, content);
            stmt.setString(4, type);
            stmt.setString(5, boxFileId);
            stmt.executeUpdate();
            
            // Update room last_message_at
            updateRoomTimestamp(roomId);
            
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new RepositoryException("Error obteniendo ID del mensaje");
        } catch (SQLException e) {
            throw new RepositoryException("Error insertando mensaje", e);
        }
    }

    private void updateRoomTimestamp(Long roomId) throws SQLException {
        String sql = "UPDATE chat_room SET last_message_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.executeUpdate();
        }
    }

    public List<ChatMessageResponse> findMessagesByRoom(Long roomId, int limit) {
        String sql = """
                SELECT m.*, u.first_name, u.last_name
                FROM chat_message m
                JOIN users u ON m.sender_id = u.id
                WHERE m.room_id = ?
                ORDER BY m.created_at DESC
                LIMIT ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                List<ChatMessageResponse> messages = new ArrayList<>();
                while (rs.next()) {
                    messages.add(mapMessage(rs));
                }
                return messages;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error buscando mensajes de la sala", e);
        }
    }

    public List<Long> getParticipantIds(Long roomId) {
        String sql = "SELECT user_id FROM chat_participant WHERE room_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getLong("user_id"));
                }
                return ids;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error buscando IDs de participantes", e);
        }
    }

    private List<ChatRoomResponse.UserSummary> findParticipantsByRoom(Long roomId) throws SQLException {
        String sql = """
                SELECT u.id, u.first_name, u.last_name, u.display_name,
                       u.avatar_box_id, p.is_typing
                FROM chat_participant p
                JOIN users u ON p.user_id = u.id
                WHERE p.room_id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<ChatRoomResponse.UserSummary> users = new ArrayList<>();
                while (rs.next()) {
                    String displayName = rs.getString("display_name");
                    String name = (displayName != null && !displayName.isBlank())
                            ? displayName
                            : rs.getString("first_name") + " " + rs.getString("last_name");
                    users.add(ChatRoomResponse.UserSummary.builder()
                            .id(rs.getLong("id"))
                            .name(name)
                            .photoUrl(rs.getString("avatar_box_id"))
                            .isTyping(rs.getBoolean("is_typing"))
                            .build());
                }
                return users;
            }
        }
    }

    private Optional<ChatMessageResponse> findLastMessageByRoom(Long roomId) throws SQLException {
        List<ChatMessageResponse> msgs = findMessagesByRoom(roomId, 1);
        return msgs.isEmpty() ? Optional.empty() : Optional.of(msgs.get(0));
    }

    private ChatMessageResponse mapMessage(ResultSet rs) throws SQLException {
        return ChatMessageResponse.builder()
                .id(rs.getLong("id"))
                .roomId(rs.getLong("room_id"))
                .senderId(rs.getLong("sender_id"))
                .senderName(rs.getString("first_name") + " " + rs.getString("last_name"))
                .content(rs.getString("content"))
                .type(rs.getString("type"))
                .boxFileId(rs.getString("box_file_id"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }

    public void hideRoom(Long roomId, Long userId) {
        String sql = "UPDATE chat_participant SET hidden_at = CURRENT_TIMESTAMP WHERE room_id = ? AND user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, userId);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new RepositoryException("No se encontró la participación en la sala");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error ocultando sala de chat", e);
        }
    }

    public void unhideRoom(Long roomId, Long userId) {
        String sql = "UPDATE chat_participant SET hidden_at = NULL WHERE room_id = ? AND user_id = ? AND hidden_at IS NOT NULL";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error restaurando sala de chat", e);
        }
    }

    public Optional<Long> findDirectChatRoom(Long userId1, Long userId2) {
        String sql = """
                SELECT r.id
                FROM chat_room r
                JOIN chat_participant p1 ON r.id = p1.room_id
                JOIN chat_participant p2 ON r.id = p2.room_id
                WHERE p1.user_id = ? AND p2.user_id = ?
                ORDER BY r.last_message_at DESC
                LIMIT 1
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId1);
            stmt.setLong(2, userId2);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getLong("id"));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error buscando chat directo", e);
        }
    }
}
