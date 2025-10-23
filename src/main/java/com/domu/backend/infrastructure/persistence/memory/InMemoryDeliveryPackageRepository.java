package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.access.DeliveryPackage;
import com.domu.backend.infrastructure.persistence.repository.DeliveryPackageRepository;

public class InMemoryDeliveryPackageRepository extends InMemoryCrudRepository<DeliveryPackage> implements DeliveryPackageRepository {
}
