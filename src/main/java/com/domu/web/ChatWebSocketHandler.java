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
            } catch (Exception e) {
                ctx.session.close();
            }
        });

        ws.onClose(ctx -> {
            userSessions.values().removeIf(session -> session.equals(ctx));
        });

        ws.onMessage(ctx -> {
            try {
                String message = ctx.message();
                Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
                String type = (String) payload.get("type");
                
                if ("TYPING".equals(type)) {
                    // Lógica de retransmisión de estado de escritura
                    // Por ahora solo log para evitar advertencias de variables no usadas
                    LOGGER.debug("User typing event received");
                }
            } catch (Exception e) {
                LOGGER.error("Error processing WS message", e);
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
}
