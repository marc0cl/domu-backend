package com.domu.backend.repository.impl;

import com.domu.backend.domain.Provider;
import com.domu.backend.repository.ProviderRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ProviderRepositoryImpl extends AbstractJpaRepository<Provider> implements ProviderRepository {

    public ProviderRepositoryImpl() {
        super(Provider.class);
    }
}
