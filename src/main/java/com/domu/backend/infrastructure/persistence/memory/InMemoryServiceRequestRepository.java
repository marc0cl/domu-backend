package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.vendor.ServiceRequest;
import com.domu.backend.infrastructure.persistence.repository.ServiceRequestRepository;

public class InMemoryServiceRequestRepository extends InMemoryCrudRepository<ServiceRequest> implements ServiceRequestRepository {
}
