package com.domu.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class VisitFromContactRequest {
    private Integer validForMinutes;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Long unitId;
    private String visitorType;
    private String company;
}

