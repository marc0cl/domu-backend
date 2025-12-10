package com.domu.dto;

import lombok.Data;

@Data
public class CreateBuildingRequest {
    private String name;
    private String towerLabel;
    private String address;
    private String commune;
    private String city;
    private String adminPhone;
    private String adminEmail;
    private String adminName;
    private String adminDocument;
    private Integer floors;
    private Integer unitsCount;
    private Double latitude;
    private Double longitude;
    private String proofText;
}

