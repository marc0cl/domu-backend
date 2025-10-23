package com.domu.backend.repository.impl;

import com.domu.backend.domain.MaintenanceSchedule;
import com.domu.backend.repository.MaintenanceScheduleRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MaintenanceScheduleRepositoryImpl extends AbstractJpaRepository<MaintenanceSchedule> implements MaintenanceScheduleRepository {

    public MaintenanceScheduleRepositoryImpl() {
        super(MaintenanceSchedule.class);
    }
}
