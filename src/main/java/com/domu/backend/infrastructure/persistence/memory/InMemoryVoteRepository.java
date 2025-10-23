package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.voting.Vote;
import com.domu.backend.infrastructure.persistence.repository.VoteRepository;

public class InMemoryVoteRepository extends InMemoryCrudRepository<Vote> implements VoteRepository {
}
