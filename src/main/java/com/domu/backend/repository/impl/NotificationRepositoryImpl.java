package com.domu.backend.repository.impl;

import com.domu.backend.domain.Notification;
import com.domu.backend.repository.NotificationRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepositoryImpl extends AbstractJpaRepository<Notification> implements NotificationRepository {

    public NotificationRepositoryImpl() {
        super(Notification.class);
    }
}
