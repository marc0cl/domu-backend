package com.domu.backend.repository.base;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class AbstractJpaRepository<T> implements BaseRepository<T, Long> {

    @PersistenceContext
    private EntityManager entityManager;

    private final Class<T> entityClass;

    protected AbstractJpaRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    @Transactional
    public T save(T entity) {
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(entityManager.find(entityClass, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        return entityManager.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAllById(Iterable<Long> ids) {
        List<Long> idList = new ArrayList<>();
        ids.forEach(idList::add);
        if (idList.isEmpty()) {
            return Collections.emptyList();
        }
        return entityManager.createQuery(
                        "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.id IN :ids",
                        entityClass)
                .setParameter("ids", idList)
                .getResultList();
    }
}
