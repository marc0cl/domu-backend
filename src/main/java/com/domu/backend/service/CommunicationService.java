package com.domu.backend.service;

import com.domu.backend.domain.community.ForumCategory;
import com.domu.backend.domain.community.ForumMessage;
import com.domu.backend.domain.community.ForumThread;
import com.domu.backend.domain.community.Notification;
import com.domu.backend.infrastructure.persistence.repository.ForumCategoryRepository;
import com.domu.backend.infrastructure.persistence.repository.ForumMessageRepository;
import com.domu.backend.infrastructure.persistence.repository.ForumThreadRepository;
import com.domu.backend.infrastructure.persistence.repository.NotificationRepository;

import java.util.List;

public class CommunicationService {

    private final ForumCategoryRepository forumCategoryRepository;
    private final ForumThreadRepository forumThreadRepository;
    private final ForumMessageRepository forumMessageRepository;
    private final NotificationRepository notificationRepository;

    public CommunicationService(ForumCategoryRepository forumCategoryRepository,
                                ForumThreadRepository forumThreadRepository,
                                ForumMessageRepository forumMessageRepository,
                                NotificationRepository notificationRepository) {
        this.forumCategoryRepository = forumCategoryRepository;
        this.forumThreadRepository = forumThreadRepository;
        this.forumMessageRepository = forumMessageRepository;
        this.notificationRepository = notificationRepository;
    }

    public ForumCategory createCategory(ForumCategory category) {
        return forumCategoryRepository.save(category);
    }

    public List<ForumCategory> listCategories() {
        return forumCategoryRepository.findAll();
    }

    public ForumThread openThread(ForumThread thread) {
        forumCategoryRepository.findById(thread.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Forum category not found"));
        return forumThreadRepository.save(thread);
    }

    public List<ForumThread> listThreads() {
        return forumThreadRepository.findAll();
    }

    public ForumMessage publishMessage(ForumMessage message) {
        forumThreadRepository.findById(message.threadId())
                .orElseThrow(() -> new ResourceNotFoundException("Forum thread not found"));
        return forumMessageRepository.save(message);
    }

    public List<ForumMessage> listMessages() {
        return forumMessageRepository.findAll();
    }

    public Notification sendNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    public List<Notification> listNotifications() {
        return notificationRepository.findAll();
    }
}
