package com.domu.dto;

import java.time.LocalDateTime;

public class CreateVisitRequest {
    private String visitorName;
    private String visitorDocument;
    private String visitorType;
    private String company;
    private Long unitId;
    private Integer validForMinutes;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    public CreateVisitRequest() {}

    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    public String getVisitorDocument() { return visitorDocument; }
    public void setVisitorDocument(String visitorDocument) { this.visitorDocument = visitorDocument; }

    public String getVisitorType() { return visitorType; }
    public void setVisitorType(String visitorType) { this.visitorType = visitorType; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public Integer getValidForMinutes() { return validForMinutes; }
    public void setValidForMinutes(Integer validForMinutes) { this.validForMinutes = validForMinutes; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
}
