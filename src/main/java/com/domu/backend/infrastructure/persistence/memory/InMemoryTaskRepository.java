package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.staff.Task;
import com.domu.backend.infrastructure.persistence.repository.TaskRepository;

public class InMemoryTaskRepository extends InMemoryCrudRepository<Task> implements TaskRepository {
}
