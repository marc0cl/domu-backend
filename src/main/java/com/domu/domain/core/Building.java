package com.domu.domain.core;

import java.time.LocalDate;
import java.util.Objects;

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
) {
    public Building {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(address, "address");
    }
}
