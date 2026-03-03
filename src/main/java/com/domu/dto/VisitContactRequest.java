package com.domu.dto;

public class VisitContactRequest {
    private String visitorName;
    private String visitorDocument;
    private Long unitId;
    private String alias;

    public VisitContactRequest() {}

    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    public String getVisitorDocument() { return visitorDocument; }
    public void setVisitorDocument(String visitorDocument) { this.visitorDocument = visitorDocument; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
}
