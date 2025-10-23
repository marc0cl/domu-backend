package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.ticket.TicketFollowUp;
import com.domu.backend.infrastructure.persistence.repository.TicketFollowUpRepository;

public class InMemoryTicketFollowUpRepository extends InMemoryCrudRepository<TicketFollowUp> implements TicketFollowUpRepository {
}
