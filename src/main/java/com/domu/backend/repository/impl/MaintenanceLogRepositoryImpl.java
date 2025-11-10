package com.domu.backend.repository.impl;

import com.domu.backend.domain.MaintenanceLog;
import com.domu.backend.repository.MaintenanceLogRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MaintenanceLogRepositoryImpl extends AbstractJpaRepository<MaintenanceLog> implements MaintenanceLogRepository {

    public MaintenanceLogRepositoryImpl() {
        super(MaintenanceLog.class);
    }
}
