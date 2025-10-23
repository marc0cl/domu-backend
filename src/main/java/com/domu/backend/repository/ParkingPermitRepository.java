package com.domu.backend.repository;

import com.domu.backend.domain.ParkingPermit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingPermitRepository extends JpaRepository<ParkingPermit, Long> {
}
