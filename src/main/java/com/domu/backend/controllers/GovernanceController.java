package com.domu.backend.controllers;

import com.domu.backend.domain.Vote;
import com.domu.backend.domain.VoteEvent;
import com.domu.backend.domain.VoteOption;
import com.domu.backend.dto.VoteEventRequest;
import com.domu.backend.dto.VoteOptionRequest;
import com.domu.backend.dto.VoteRequest;
import com.domu.backend.services.GovernanceService;
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

    private final GovernanceService governanceService;

    public GovernanceController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @PostMapping("/events")
    public VoteEvent createVoteEvent(@Valid @RequestBody VoteEventRequest request) {
        return governanceService.createVoteEvent(request);
    }

    @GetMapping("/events")
    public List<VoteEvent> listVoteEvents() {
        return governanceService.listVoteEvents();
    }

    @PostMapping("/options")
    public VoteOption createVoteOption(@Valid @RequestBody VoteOptionRequest request) {
        return governanceService.createVoteOption(request);
    }

    @GetMapping("/options")
    public List<VoteOption> listVoteOptions() {
        return governanceService.listVoteOptions();
    }

    @PostMapping("/votes")
    public Vote createVote(@Valid @RequestBody VoteRequest request) {
        return governanceService.createVote(request);
    }

    @GetMapping("/votes")
    public List<Vote> listVotes() {
        return governanceService.listVotes();
    }
}
