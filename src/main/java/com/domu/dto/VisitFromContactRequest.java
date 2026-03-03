package com.domu.dto;

import java.time.LocalDateTime;

public class VisitFromContactRequest {
    private Integer validForMinutes;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Long unitId;
    private String visitorType;
    private String company;

    public VisitFromContactRequest() {}

    public Integer getValidForMinutes() { return validForMinutes; }
    public void setValidForMinutes(Integer validForMinutes) { this.validForMinutes = validForMinutes; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public String getVisitorType() { return visitorType; }
    public void setVisitorType(String visitorType) { this.visitorType = visitorType; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
}
