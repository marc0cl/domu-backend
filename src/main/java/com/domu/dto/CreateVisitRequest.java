package com.domu.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CreateVisitRequest {
    private String visitorName;
    private String visitorDocument;
    private String visitorType;
    private String company;
    private Long unitId;
    private Integer validForMinutes;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
}

