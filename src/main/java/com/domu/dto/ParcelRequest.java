package com.domu.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParcelRequest {
    private Long unitId;
    private String sender;
    private String description;
    private LocalDateTime receivedAt;
}
