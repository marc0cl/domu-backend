package com.domu.backend.repository.impl;

import com.domu.backend.domain.TaskAttachment;
import com.domu.backend.repository.TaskAttachmentRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TaskAttachmentRepositoryImpl extends AbstractJpaRepository<TaskAttachment> implements TaskAttachmentRepository {

    public TaskAttachmentRepositoryImpl() {
        super(TaskAttachment.class);
    }
}
