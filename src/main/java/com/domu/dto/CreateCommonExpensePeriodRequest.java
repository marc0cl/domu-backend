package com.domu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateCommonExpensePeriodRequest {
    private Long buildingId;
    private Integer year;
    private Integer month;
    private LocalDate dueDate;
    private BigDecimal reserveAmount;
    private List<CreateCommonChargeRequest> charges;
    private String note;

    public CreateCommonExpensePeriodRequest() {}

    public Long getBuildingId() { return buildingId; }
    public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getReserveAmount() { return reserveAmount; }
    public void setReserveAmount(BigDecimal reserveAmount) { this.reserveAmount = reserveAmount; }

    public List<CreateCommonChargeRequest> getCharges() { return charges; }
    public void setCharges(List<CreateCommonChargeRequest> charges) { this.charges = charges; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
