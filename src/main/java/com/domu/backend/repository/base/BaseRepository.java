package com.domu.backend.repository.base;

import java.util.List;
import java.util.Optional;

public interface BaseRepository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    List<T> findAllById(Iterable<ID> ids);
}
