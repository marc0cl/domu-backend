package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.voting.VotingEvent;
import com.domu.backend.infrastructure.persistence.repository.VotingEventRepository;

public class InMemoryVotingEventRepository extends InMemoryCrudRepository<VotingEvent> implements VotingEventRepository {
}
