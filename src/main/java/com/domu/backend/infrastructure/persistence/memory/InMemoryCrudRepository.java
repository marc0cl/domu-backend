package com.domu.backend.infrastructure.persistence.memory;

import com.domu.backend.domain.Identifiable;
import com.domu.backend.infrastructure.persistence.repository.CrudRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class InMemoryCrudRepository<T extends Identifiable<T>> implements CrudRepository<T> {
    private final Map<Long, T> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public T save(T entity) {
        T persisted = entity;
        if (entity.id() == null) {
            long id = sequence.incrementAndGet();
            persisted = entity.withId(id);
        }
        store.put(persisted.id(), persisted);
        return persisted;
    }

    @Override
    public Optional<T> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(Long id) {
        if (id != null) {
            store.remove(id);
        }
    }
}
