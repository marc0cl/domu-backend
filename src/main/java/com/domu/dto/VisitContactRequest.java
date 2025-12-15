package com.domu.dto;

import lombok.Data;

@Data
public class VisitContactRequest {
    private String visitorName;
    private String visitorDocument;
    private Long unitId;
    private String alias;
}

