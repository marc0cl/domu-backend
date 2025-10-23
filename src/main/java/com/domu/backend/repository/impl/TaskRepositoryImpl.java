package com.domu.backend.repository.impl;

import com.domu.backend.domain.Task;
import com.domu.backend.repository.TaskRepository;
import com.domu.backend.repository.base.AbstractJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepositoryImpl extends AbstractJpaRepository<Task> implements TaskRepository {

    public TaskRepositoryImpl() {
        super(Task.class);
    }
}
