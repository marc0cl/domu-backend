package com.domu.backend.web;

import com.domu.backend.domain.Community;
import com.domu.backend.domain.Resident;
import com.domu.backend.domain.Vote;
import com.domu.backend.domain.VoteEvent;
import com.domu.backend.domain.VoteOption;
import com.domu.backend.dto.VoteEventRequest;
import com.domu.backend.dto.VoteOptionRequest;
import com.domu.backend.dto.VoteRequest;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.ResidentRepository;
import com.domu.backend.repository.VoteEventRepository;
import com.domu.backend.repository.VoteOptionRepository;
import com.domu.backend.repository.VoteRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/governance")
public class GovernanceController {

    private final VoteEventRepository voteEventRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteRepository voteRepository;
    private final CommunityRepository communityRepository;
    private final ResidentRepository residentRepository;

    public GovernanceController(VoteEventRepository voteEventRepository,
                                VoteOptionRepository voteOptionRepository,
                                VoteRepository voteRepository,
                                CommunityRepository communityRepository,
                                ResidentRepository residentRepository) {
        this.voteEventRepository = voteEventRepository;
        this.voteOptionRepository = voteOptionRepository;
        this.voteRepository = voteRepository;
        this.communityRepository = communityRepository;
        this.residentRepository = residentRepository;
    }

    @PostMapping("/events")
    public VoteEvent createVoteEvent(@Valid @RequestBody VoteEventRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        VoteEvent event = new VoteEvent();
        event.setCommunity(community);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        if (request.anonymous() != null) {
            event.setAnonymous(request.anonymous());
        }
        return voteEventRepository.save(event);
    }

    @GetMapping("/events")
    public List<VoteEvent> listVoteEvents() {
        return voteEventRepository.findAll();
    }

    @PostMapping("/options")
    public VoteOption createVoteOption(@Valid @RequestBody VoteOptionRequest request) {
        VoteEvent event = voteEventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Vote event not found"));
        VoteOption option = new VoteOption();
        option.setEvent(event);
        option.setLabel(request.label());
        return voteOptionRepository.save(option);
    }

    @GetMapping("/options")
    public List<VoteOption> listVoteOptions() {
        return voteOptionRepository.findAll();
    }

    @PostMapping("/votes")
    public Vote createVote(@Valid @RequestBody VoteRequest request) {
        VoteEvent event = voteEventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Vote event not found"));
        VoteOption option = voteOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new ResourceNotFoundException("Vote option not found"));
        Resident resident = residentRepository.findById(request.residentId())
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
        Vote vote = new Vote();
        vote.setEvent(event);
        vote.setOption(option);
        vote.setResident(resident);
        if (request.castAt() != null) {
            vote.setCastAt(request.castAt());
        }
        vote.setVerificationHash(request.verificationHash());
        return voteRepository.save(vote);
    }

    @GetMapping("/votes")
    public List<Vote> listVotes() {
        return voteRepository.findAll();
    }
}
