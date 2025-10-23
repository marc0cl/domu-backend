package com.domu.backend.repository;

import com.domu.backend.domain.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {
}
