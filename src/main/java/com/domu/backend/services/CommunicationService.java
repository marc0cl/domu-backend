package com.domu.backend.services;

import com.domu.backend.domain.Community;
import com.domu.backend.domain.ForumCategory;
import com.domu.backend.domain.ForumPost;
import com.domu.backend.domain.ForumThread;
import com.domu.backend.domain.Notification;
import com.domu.backend.domain.Resident;
import com.domu.backend.domain.Ticket;
import com.domu.backend.domain.TicketUpdate;
import com.domu.backend.dto.ForumCategoryRequest;
import com.domu.backend.dto.ForumPostRequest;
import com.domu.backend.dto.ForumThreadRequest;
import com.domu.backend.dto.NotificationRequest;
import com.domu.backend.dto.TicketRequest;
import com.domu.backend.dto.TicketUpdateRequest;
import com.domu.backend.exceptions.ResourceNotFoundException;
import com.domu.backend.repository.CommunityRepository;
import com.domu.backend.repository.ForumCategoryRepository;
import com.domu.backend.repository.ForumPostRepository;
import com.domu.backend.repository.ForumThreadRepository;
import com.domu.backend.repository.NotificationRepository;
import com.domu.backend.repository.ResidentRepository;
import com.domu.backend.repository.TicketRepository;
import com.domu.backend.repository.TicketUpdateRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommunicationService {

    private final ForumCategoryRepository forumCategoryRepository;
    private final ForumThreadRepository forumThreadRepository;
    private final ForumPostRepository forumPostRepository;
    private final TicketRepository ticketRepository;
    private final TicketUpdateRepository ticketUpdateRepository;
    private final NotificationRepository notificationRepository;
    private final CommunityRepository communityRepository;
    private final ResidentRepository residentRepository;

    public CommunicationService(ForumCategoryRepository forumCategoryRepository,
                                ForumThreadRepository forumThreadRepository,
                                ForumPostRepository forumPostRepository,
                                TicketRepository ticketRepository,
                                TicketUpdateRepository ticketUpdateRepository,
                                NotificationRepository notificationRepository,
                                CommunityRepository communityRepository,
                                ResidentRepository residentRepository) {
        this.forumCategoryRepository = forumCategoryRepository;
        this.forumThreadRepository = forumThreadRepository;
        this.forumPostRepository = forumPostRepository;
        this.ticketRepository = ticketRepository;
        this.ticketUpdateRepository = ticketUpdateRepository;
        this.notificationRepository = notificationRepository;
        this.communityRepository = communityRepository;
        this.residentRepository = residentRepository;
    }

    public ForumCategory createForumCategory(ForumCategoryRequest request) {
        ForumCategory category = new ForumCategory();
        category.setName(request.name());
        category.setDescription(request.description());
        return forumCategoryRepository.save(category);
    }

    public List<ForumCategory> listForumCategories() {
        return forumCategoryRepository.findAll();
    }

    public ForumThread createForumThread(ForumThreadRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Resident author = residentRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
        ForumThread thread = new ForumThread();
        thread.setCommunity(community);
        if (request.categoryId() != null) {
            ForumCategory category = forumCategoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Forum category not found"));
            thread.setCategory(category);
        }
        thread.setAuthor(author);
        thread.setTitle(request.title());
        if (request.createdAt() != null) {
            thread.setCreatedAt(request.createdAt());
        }
        return forumThreadRepository.save(thread);
    }

    public List<ForumThread> listForumThreads() {
        return forumThreadRepository.findAll();
    }

    public ForumPost createForumPost(ForumPostRequest request) {
        ForumThread thread = forumThreadRepository.findById(request.threadId())
                .orElseThrow(() -> new ResourceNotFoundException("Forum thread not found"));
        Resident author = residentRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
        ForumPost post = new ForumPost();
        post.setThread(thread);
        post.setAuthor(author);
        post.setContent(request.content());
        if (request.createdAt() != null) {
            post.setCreatedAt(request.createdAt());
        }
        return forumPostRepository.save(post);
    }

    public List<ForumPost> listForumPosts() {
        return forumPostRepository.findAll();
    }

    public Ticket createTicket(TicketRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Resident reporter = residentRepository.findById(request.reporterId())
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
        Ticket ticket = new Ticket();
        ticket.setCommunity(community);
        ticket.setReporter(reporter);
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setCategory(request.category());
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.status() != null) {
            ticket.setStatus(request.status());
        }
        if (request.createdAt() != null) {
            ticket.setCreatedAt(request.createdAt());
        }
        return ticketRepository.save(ticket);
    }

    public List<Ticket> listTickets() {
        return ticketRepository.findAll();
    }

    public TicketUpdate createTicketUpdate(TicketUpdateRequest request) {
        Ticket ticket = ticketRepository.findById(request.ticketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        TicketUpdate update = new TicketUpdate();
        update.setTicket(ticket);
        if (request.authorId() != null) {
            Resident author = residentRepository.findById(request.authorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
            update.setAuthor(author);
        }
        update.setStatus(request.status());
        update.setMessage(request.message());
        if (request.createdAt() != null) {
            update.setCreatedAt(request.createdAt());
        }
        return ticketUpdateRepository.save(update);
    }

    public List<TicketUpdate> listTicketUpdates() {
        return ticketUpdateRepository.findAll();
    }

    public Notification createNotification(NotificationRequest request) {
        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        Notification notification = new Notification();
        notification.setCommunity(community);
        if (request.residentId() != null) {
            Resident recipient = residentRepository.findById(request.residentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
            notification.setRecipient(recipient);
        }
        notification.setTitle(request.title());
        notification.setMessage(request.message());
        if (request.channel() != null) {
            notification.setChannel(request.channel());
        }
        notification.setData(request.data());
        if (request.createdAt() != null) {
            notification.setCreatedAt(request.createdAt());
        }
        notification.setDeliveredAt(request.deliveredAt());
        return notificationRepository.save(notification);
    }

    public List<Notification> listNotifications() {
        return notificationRepository.findAll();
    }
}
