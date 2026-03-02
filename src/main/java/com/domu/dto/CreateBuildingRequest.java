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
    private String buildingType;
    private Integer floors;
    private Integer unitsCount;
    private Integer houseUnitsCount;
    private Integer apartmentUnitsCount;
    private Double latitude;
    private Double longitude;
    private String proofText;
}

