package com.domu.backend.domain;

public interface Identifiable<T extends Identifiable<T>> {
    Long id();

    T withId(Long id);
}
