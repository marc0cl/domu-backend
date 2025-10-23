package com.domu.backend.repository.impl;

import com.domu.backend.domain.ProviderRequest;
import com.domu.backend.repository.ProviderRequestRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ProviderRequestRepositoryImpl extends AbstractJpaRepository<ProviderRequest> implements ProviderRequestRepository {

    public ProviderRequestRepositoryImpl() {
        super(ProviderRequest.class);
    }
}
