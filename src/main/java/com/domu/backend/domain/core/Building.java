package com.domu.backend.domain.core;

import java.time.LocalDate;
import java.util.Objects;

import com.domu.backend.domain.Identifiable;

public record Building(
        Long id,
        String name,
        String address,
        String commune,
        String city,
        String adminPhone,
        String adminEmail,
        LocalDate createdAt,
        String status
) implements Identifiable<Building> {
    public Building {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(address, "address");
    }

    @Override
    public Building withId(Long newId) {
        return new Building(
                newId,
                name,
                address,
                commune,
                city,
                adminPhone,
                adminEmail,
                createdAt,
                status
        );
    }
}
