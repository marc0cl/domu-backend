package com.domu.dto;

import java.util.List;

import lombok.Data;

@Data
public class AddCommonChargesRequest {
    private List<CreateCommonChargeRequest> charges;
    private String note;
}
