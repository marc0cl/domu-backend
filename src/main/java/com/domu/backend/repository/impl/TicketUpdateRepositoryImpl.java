package com.domu.backend.repository.impl;

import com.domu.backend.domain.TicketUpdate;
import com.domu.backend.repository.TicketUpdateRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TicketUpdateRepositoryImpl extends AbstractJpaRepository<TicketUpdate> implements TicketUpdateRepository {

    public TicketUpdateRepositoryImpl() {
        super(TicketUpdate.class);
    }
}
