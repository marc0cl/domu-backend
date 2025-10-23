package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.community.Notification;
import com.domu.backend.infrastructure.persistence.repository.NotificationRepository;

public class InMemoryNotificationRepository extends InMemoryCrudRepository<Notification> implements NotificationRepository {
}
