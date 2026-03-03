package com.domu.dto;

import java.util.List;

public class AddCommonChargesRequest {
    private List<CreateCommonChargeRequest> charges;
    private String note;

    public AddCommonChargesRequest() {}

    public List<CreateCommonChargeRequest> getCharges() { return charges; }
    public void setCharges(List<CreateCommonChargeRequest> charges) { this.charges = charges; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
