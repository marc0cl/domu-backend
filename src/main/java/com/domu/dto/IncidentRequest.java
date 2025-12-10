package com.domu.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class IncidentRequest {
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private LocalDateTime createdAt;
}

