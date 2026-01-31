package com.domu.dto;

import java.util.List;

public record HousingUnitWithResidents(
                HousingUnitResponse unit,
                List<ResidentSummary> residents) {
        public record ResidentSummary(
                        Long id,
                        String firstName,
                        String lastName,
                        String email,
                        String phone,
                        String documentNumber) {
        }
}
