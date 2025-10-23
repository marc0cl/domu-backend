package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.vendor.PreventiveMaintenance;
import com.domu.backend.infrastructure.persistence.repository.PreventiveMaintenanceRepository;

public class InMemoryPreventiveMaintenanceRepository extends InMemoryCrudRepository<PreventiveMaintenance> implements PreventiveMaintenanceRepository {
}
