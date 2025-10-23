package com.domu.backend.infrastructure.persistence.repository;

import com.domu.backend.domain.Identifiable;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T extends Identifiable<T>> {
    T save(T entity);

    Optional<T> findById(Long id);

    List<T> findAll();

    void deleteById(Long id);
}
