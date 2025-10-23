package com.domu.backend.controllers;

import com.domu.backend.domain.ForumCategory;
import com.domu.backend.domain.ForumPost;
import com.domu.backend.domain.ForumThread;
import com.domu.backend.domain.Notification;
import com.domu.backend.domain.Ticket;
import com.domu.backend.domain.TicketUpdate;
import com.domu.backend.dto.ForumCategoryRequest;
import com.domu.backend.dto.ForumPostRequest;
import com.domu.backend.dto.ForumThreadRequest;
import com.domu.backend.dto.NotificationRequest;
import com.domu.backend.dto.TicketRequest;
import com.domu.backend.dto.TicketUpdateRequest;
import com.domu.backend.services.CommunicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/communication")
public class CommunicationController {

    private final CommunicationService communicationService;

    public CommunicationController(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @PostMapping("/forum/categories")
    public ForumCategory createForumCategory(@Valid @RequestBody ForumCategoryRequest request) {
        return communicationService.createForumCategory(request);
    }

    @GetMapping("/forum/categories")
    public List<ForumCategory> listForumCategories() {
        return communicationService.listForumCategories();
    }

    @PostMapping("/forum/threads")
    public ForumThread createForumThread(@Valid @RequestBody ForumThreadRequest request) {
        return communicationService.createForumThread(request);
    }

    @GetMapping("/forum/threads")
    public List<ForumThread> listForumThreads() {
        return communicationService.listForumThreads();
    }

    @PostMapping("/forum/posts")
    public ForumPost createForumPost(@Valid @RequestBody ForumPostRequest request) {
        return communicationService.createForumPost(request);
    }

    @GetMapping("/forum/posts")
    public List<ForumPost> listForumPosts() {
        return communicationService.listForumPosts();
    }

    @PostMapping("/tickets")
    public Ticket createTicket(@Valid @RequestBody TicketRequest request) {
        return communicationService.createTicket(request);
    }

    @GetMapping("/tickets")
    public List<Ticket> listTickets() {
        return communicationService.listTickets();
    }

    @PostMapping("/ticket-updates")
    public TicketUpdate createTicketUpdate(@Valid @RequestBody TicketUpdateRequest request) {
        return communicationService.createTicketUpdate(request);
    }

    @GetMapping("/ticket-updates")
    public List<TicketUpdate> listTicketUpdates() {
        return communicationService.listTicketUpdates();
    }

    @PostMapping("/notifications")
    public Notification createNotification(@Valid @RequestBody NotificationRequest request) {
        return communicationService.createNotification(request);
    }

    @GetMapping("/notifications")
    public List<Notification> listNotifications() {
        return communicationService.listNotifications();
    }
}
