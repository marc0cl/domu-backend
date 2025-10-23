package com.domu.backend.repository.impl;

import com.domu.backend.domain.Ticket;
import com.domu.backend.repository.TicketRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TicketRepositoryImpl extends AbstractJpaRepository<Ticket> implements TicketRepository {

    public TicketRepositoryImpl() {
        super(Ticket.class);
    }
}
