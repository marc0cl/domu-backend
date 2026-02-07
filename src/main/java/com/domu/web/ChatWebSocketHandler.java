package com.domu.web;

import com.domu.dto.ChatMessageResponse;
import com.domu.security.JwtProvider;
import com.domu.service.ChatService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ChatWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final Map<Long, WsContext> userSessions = new ConcurrentHashMap<>();
    private final ChatService chatService;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Inject
    public ChatWebSocketHandler(ChatService chatService, JwtProvider jwtProvider, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the set of user IDs currently connected via WebSocket.
     */
    public Set<Long> getOnlineUserIds() {
        userSessions.entrySet().removeIf(entry -> !entry.getValue().session.isOpen());
        return Set.copyOf(userSessions.keySet());
    }

    public void handle(io.javalin.websocket.WsConfig ws) {
        ws.onConnect(ctx -> {
            String token = ctx.queryParam("token");
            if (token == null) {
                ctx.session.close();
                return;
            }
            try {
                Long userId = Long.parseLong(jwtProvider.verify(token).getSubject());
                userSessions.put(userId, ctx);
                LOGGER.info("User {} connected to chat WS", userId);
                broadcastPresence(userId, true);
            } catch (Exception e) {
                ctx.session.close();
            }
        });

        ws.onClose(ctx -> {
            Long disconnectedUserId = null;
            for (Map.Entry<Long, WsContext> entry : userSessions.entrySet()) {
                if (entry.getValue().equals(ctx)) {
                    disconnectedUserId = entry.getKey();
                    break;
                }
            }
            userSessions.values().removeIf(session -> session.equals(ctx));
            if (disconnectedUserId != null) {
                LOGGER.info("User {} disconnected from chat WS", disconnectedUserId);
                broadcastPresence(disconnectedUserId, false);
            }
        });

        ws.onMessage(ctx -> {
            try {
                String message = ctx.message();
                Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
                String type = (String) payload.get("type");
                
                if ("SEND_MSG".equals(type)) {
                    Long userId = null;
                    for (Map.Entry<Long, WsContext> entry : userSessions.entrySet()) {
                        if (entry.getValue().equals(ctx)) {
                            userId = entry.getKey();
                            break;
                        }
                    }
                    if (userId == null) return;

                    Long roomId = Long.parseLong(payload.get("roomId").toString());
                    String content = (String) payload.get("content");
                    String msgType = (String) payload.get("msgType");

                    ChatMessageResponse savedMsg = chatService.saveMessage(roomId, userId, content, msgType, null, null);
                    
                    List<Long> participants = chatService.getParticipantIds(roomId);
                    notifyNewMessage(roomId, savedMsg, participants);
                } else if ("TYPING".equals(type)) {
                    // TODO: Handle typing
                }
            } catch (Exception e) {
                LOGGER.error("Error handling WS message", e);
            }
        });
    }

    public void notifyNewMessage(Long roomId, ChatMessageResponse message, List<Long> participantIds) {
        Map<String, Object> response = Map.of(
            "type", "NEW_MESSAGE",
            "roomId", roomId,
            "message", message
        );
        
        participantIds.forEach(id -> {
            WsContext session = userSessions.get(id);
            if (session != null && session.session.isOpen()) {
                try {
                    session.send(response);
                } catch (Exception e) {
                    LOGGER.error("Error sending message via WS to user {}", id);
                }
            }
        });
    }

    /**
     * Broadcasts a PRESENCE event to all connected users when someone goes online/offline.
     */
    private void broadcastPresence(Long userId, boolean online) {
        Map<String, Object> event = Map.of(
            "type", "PRESENCE",
            "userId", userId,
            "online", online
        );
        userSessions.forEach((id, session) -> {
            if (session.session.isOpen()) {
                try {
                    session.send(event);
                } catch (Exception e) {
                    LOGGER.debug("Error broadcasting presence to user {}", id);
                }
            }
        });
    }
}
