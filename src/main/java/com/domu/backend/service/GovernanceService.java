package com.domu.backend.service;

import com.domu.backend.domain.voting.Vote;
import com.domu.backend.domain.voting.VotingEvent;
import com.domu.backend.domain.voting.VotingOption;
import com.domu.backend.infrastructure.persistence.repository.VoteRepository;
import com.domu.backend.infrastructure.persistence.repository.VotingEventRepository;
import com.domu.backend.infrastructure.persistence.repository.VotingOptionRepository;

import java.util.List;

public class GovernanceService {

    private final VotingEventRepository votingEventRepository;
    private final VotingOptionRepository votingOptionRepository;
    private final VoteRepository voteRepository;

    public GovernanceService(VotingEventRepository votingEventRepository,
                             VotingOptionRepository votingOptionRepository,
                             VoteRepository voteRepository) {
        this.votingEventRepository = votingEventRepository;
        this.votingOptionRepository = votingOptionRepository;
        this.voteRepository = voteRepository;
    }

    public VotingEvent createVotingEvent(VotingEvent event) {
        return votingEventRepository.save(event);
    }

    public List<VotingEvent> listVotingEvents() {
        return votingEventRepository.findAll();
    }

    public VotingOption addOption(VotingOption option) {
        votingEventRepository.findById(option.votingEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Voting event not found"));
        return votingOptionRepository.save(option);
    }

    public List<VotingOption> listOptions() {
        return votingOptionRepository.findAll();
    }

    public Vote castVote(Vote vote) {
        votingEventRepository.findById(vote.votingEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Voting event not found"));
        votingOptionRepository.findById(vote.optionId())
                .orElseThrow(() -> new ResourceNotFoundException("Voting option not found"));
        return voteRepository.save(vote);
    }

    public List<Vote> listVotes() {
        return voteRepository.findAll();
    }
}
