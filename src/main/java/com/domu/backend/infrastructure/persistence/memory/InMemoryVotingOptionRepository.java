package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.voting.VotingOption;
import com.domu.backend.infrastructure.persistence.repository.VotingOptionRepository;

public class InMemoryVotingOptionRepository extends InMemoryCrudRepository<VotingOption> implements VotingOptionRepository {
}
