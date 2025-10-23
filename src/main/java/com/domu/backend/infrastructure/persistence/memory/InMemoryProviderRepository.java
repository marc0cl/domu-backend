package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.vendor.Provider;
import com.domu.backend.infrastructure.persistence.repository.ProviderRepository;

public class InMemoryProviderRepository extends InMemoryCrudRepository<Provider> implements ProviderRepository {
}
