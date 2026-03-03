package com.domu.service;

import com.domu.database.NotificationRepository;
import com.domu.database.UserBuildingRepository;
import com.domu.domain.NotificationType;
import com.domu.dto.NotificationPreferenceResponse;
import com.domu.dto.NotificationResponse;
import com.domu.web.NotificationWebSocketHandler;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private static final Map<String, String> TYPE_LABELS = Map.ofEntries(
        Map.entry("INCIDENT_CREATED", "Incidente creado"),
        Map.entry("INCIDENT_ASSIGNED", "Incidente asignado"),
        Map.entry("INCIDENT_STATUS_CHANGED", "Estado de incidente"),
        Map.entry("VISIT_AUTHORIZED", "Visita autorizada"),
        Map.entry("VISIT_CHECKED_IN", "Check-in de visita"),
        Map.entry("PARCEL_RECEIVED", "Encomienda recibida"),
        Map.entry("PARCEL_COLLECTED", "Encomienda retirada"),
        Map.entry("CHARGE_PERIOD_CREATED", "Gasto comun"),
        Map.entry("PAYMENT_CONFIRMED", "Pago confirmado"),
        Map.entry("TASK_ASSIGNED", "Tarea asignada"),
        Map.entry("TASK_COMPLETED", "Tarea completada"),
        Map.entry("POLL_CREATED", "Nueva votacion"),
        Map.entry("POLL_CLOSED", "Votacion cerrada"),
        Map.entry("RESERVATION_CONFIRMED", "Reserva confirmada"),
        Map.entry("RESERVATION_CANCELLED", "Reserva cancelada"),
        Map.entry("FORUM_THREAD_CREATED", "Nueva publicacion"),
        Map.entry("ADMIN_ANNOUNCEMENT", "Aviso de administracion"),
        Map.entry("MARKET_ITEM_CREATED", "Nuevo articulo en marketplace"),
        Map.entry("CHAT_REQUEST_RECEIVED", "Solicitud de chat"),
        Map.entry("CHAT_REQUEST_ACCEPTED", "Chat aceptado"),
        Map.entry("MAINTENANCE_SCHEDULED", "Mantenimiento programado"),
        Map.entry("MAINTENANCE_COMPLETED", "Mantenimiento completado")
    );

    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketHandler webSocketHandler;
    private final UserBuildingRepository userBuildingRepository;

    @Inject
    public NotificationService(NotificationRepository notificationRepository,
                               NotificationWebSocketHandler webSocketHandler,
                               UserBuildingRepository userBuildingRepository) {
        this.notificationRepository = notificationRepository;
        this.webSocketHandler = webSocketHandler;
        this.userBuildingRepository = userBuildingRepository;
    }

    public void notify(Long buildingId, Long userId, NotificationType type, String title, String message, String dataJson) {
        CompletableFuture.runAsync(() -> {
            try {
                if (!isInAppEnabled(userId, type.name())) return;
                LocalDateTime now = LocalDateTime.now();
                var row = notificationRepository.insert(new NotificationRepository.NotificationRow(
                    null, buildingId, userId, type.name(), title, message, dataJson, false, now, null
                ));
                NotificationResponse response = toResponse(row);
                webSocketHandler.sendNotification(userId, response);
            } catch (Exception e) {
                LOGGER.error("Error sending notification to user {}: {}", userId, e.getMessage(), e);
            }
        });
    }

    public void notifyBuildingUsers(Long buildingId, List<Long> userIds, NotificationType type,
                                     String title, String message, String dataJson) {
        CompletableFuture.runAsync(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                List<NotificationRepository.NotificationRow> rows = new ArrayList<>();
                for (Long userId : userIds) {
                    if (!isInAppEnabled(userId, type.name())) continue;
                    rows.add(new NotificationRepository.NotificationRow(
                        null, buildingId, userId, type.name(), title, message, dataJson, false, now, null
                    ));
                }
                if (rows.isEmpty()) return;
                notificationRepository.insertBatch(rows);
                for (NotificationRepository.NotificationRow row : rows) {
                    if (webSocketHandler.isUserConnected(row.userId())) {
                        NotificationResponse response = new NotificationResponse(
                            null, row.buildingId(), row.type(), row.title(), row.message(),
                            row.data(), false, row.createdAt(), null
                        );
                        webSocketHandler.sendNotification(row.userId(), response);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error sending batch notification: {}", e.getMessage(), e);
            }
        });
    }

    public void notifyBuildingUsersByRoles(Long buildingId, List<Long> roleIds, NotificationType type,
                                            String title, String message, String dataJson) {
        List<Long> userIds = userBuildingRepository.findUserIdsByBuildingAndRoles(buildingId, roleIds);
        notifyBuildingUsers(buildingId, userIds, type, title, message, dataJson);
    }

    public void notifyAllBuildingUsers(Long buildingId, NotificationType type, String title,
                                        String message, String dataJson, Long excludeUserId) {
        List<Long> userIds = userBuildingRepository.findUserIdsByBuilding(buildingId);
        if (excludeUserId != null) {
            userIds.remove(excludeUserId);
        }
        notifyBuildingUsers(buildingId, userIds, type, title, message, dataJson);
    }

    public List<NotificationResponse> getNotifications(Long userId, Long buildingId, int page, int size) {
        int offset = page * size;
        return notificationRepository.findByUserAndBuilding(userId, buildingId, size, offset)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public int getUnreadCount(Long userId, Long buildingId) {
        return notificationRepository.countUnread(userId, buildingId);
    }

    public boolean markRead(Long notificationId, Long userId) {
        return notificationRepository.markRead(notificationId, userId);
    }

    public int markAllRead(Long userId, Long buildingId) {
        return notificationRepository.markAllRead(userId, buildingId);
    }

    public List<NotificationPreferenceResponse> getPreferences(Long userId) {
        List<NotificationRepository.PreferenceRow> saved = notificationRepository.findPreferences(userId);
        Map<String, Boolean> savedMap = saved.stream()
                .collect(Collectors.toMap(NotificationRepository.PreferenceRow::notificationType,
                        NotificationRepository.PreferenceRow::inAppEnabled));

        List<NotificationPreferenceResponse> result = new ArrayList<>();
        for (NotificationType type : NotificationType.values()) {
            String name = type.name();
            boolean enabled = savedMap.getOrDefault(name, true);
            String label = TYPE_LABELS.getOrDefault(name, name);
            result.add(new NotificationPreferenceResponse(name, label, enabled));
        }
        return result;
    }

    public void updatePreference(Long userId, String notificationType, boolean inAppEnabled) {
        notificationRepository.upsertPreference(userId, notificationType, inAppEnabled);
    }

    private boolean isInAppEnabled(Long userId, String type) {
        List<NotificationRepository.PreferenceRow> prefs = notificationRepository.findPreferences(userId);
        for (NotificationRepository.PreferenceRow pref : prefs) {
            if (pref.notificationType().equals(type)) {
                return pref.inAppEnabled();
            }
        }
        return true; // enabled by default
    }

    private NotificationResponse toResponse(NotificationRepository.NotificationRow row) {
        return new NotificationResponse(
            row.id(), row.buildingId(), row.type(), row.title(), row.message(),
            row.data(), row.isRead(), row.createdAt(), row.readAt()
        );
    }
}
