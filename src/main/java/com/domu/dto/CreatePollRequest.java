package com.domu.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class CreatePollRequest {
    private String title;
    private String description;
    private LocalDateTime closesAt;
    private List<String> options;
    private Long buildingId;
}
