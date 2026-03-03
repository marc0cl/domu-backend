package com.domu.web;

import com.domu.dto.NotificationResponse;
import com.domu.security.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class NotificationWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private final Map<Long, WsContext> userSessions = new ConcurrentHashMap<>();
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Inject
    public NotificationWebSocketHandler(JwtProvider jwtProvider, ObjectMapper objectMapper) {
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
                LOGGER.info("User {} connected to notifications WS", userId);
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
                LOGGER.info("User {} disconnected from notifications WS", disconnectedUserId);
            }
        });

        ws.onMessage(ctx -> {
            // Server-push only channel, no client messages expected
        });
    }

    public void sendNotification(Long userId, NotificationResponse notification) {
        WsContext session = userSessions.get(userId);
        if (session != null && session.session.isOpen()) {
            try {
                Map<String, Object> payload = Map.of(
                    "type", "NOTIFICATION",
                    "notification", notification
                );
                session.send(objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                LOGGER.error("Error sending notification via WS to user {}", userId, e);
            }
        }
    }

    public void sendUnreadCount(Long userId, int count) {
        WsContext session = userSessions.get(userId);
        if (session != null && session.session.isOpen()) {
            try {
                Map<String, Object> payload = Map.of(
                    "type", "UNREAD_COUNT",
                    "count", count
                );
                session.send(objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                LOGGER.error("Error sending unread count via WS to user {}", userId, e);
            }
        }
    }

    public boolean isUserConnected(Long userId) {
        WsContext session = userSessions.get(userId);
        return session != null && session.session.isOpen();
    }
}
