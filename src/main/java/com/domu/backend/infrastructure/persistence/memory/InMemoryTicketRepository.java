package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.ticket.Ticket;
import com.domu.backend.infrastructure.persistence.repository.TicketRepository;

public class InMemoryTicketRepository extends InMemoryCrudRepository<Ticket> implements TicketRepository {
}
