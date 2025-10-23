package com.domu.backend.repository;

import com.domu.backend.domain.TicketUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketUpdateRepository extends JpaRepository<TicketUpdate, Long> {
}
