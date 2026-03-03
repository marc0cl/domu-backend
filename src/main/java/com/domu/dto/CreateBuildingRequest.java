package com.domu.dto;

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

    public CreateBuildingRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTowerLabel() { return towerLabel; }
    public void setTowerLabel(String towerLabel) { this.towerLabel = towerLabel; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCommune() { return commune; }
    public void setCommune(String commune) { this.commune = commune; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAdminPhone() { return adminPhone; }
    public void setAdminPhone(String adminPhone) { this.adminPhone = adminPhone; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getAdminDocument() { return adminDocument; }
    public void setAdminDocument(String adminDocument) { this.adminDocument = adminDocument; }

    public String getBuildingType() { return buildingType; }
    public void setBuildingType(String buildingType) { this.buildingType = buildingType; }

    public Integer getFloors() { return floors; }
    public void setFloors(Integer floors) { this.floors = floors; }

    public Integer getUnitsCount() { return unitsCount; }
    public void setUnitsCount(Integer unitsCount) { this.unitsCount = unitsCount; }

    public Integer getHouseUnitsCount() { return houseUnitsCount; }
    public void setHouseUnitsCount(Integer houseUnitsCount) { this.houseUnitsCount = houseUnitsCount; }

    public Integer getApartmentUnitsCount() { return apartmentUnitsCount; }
    public void setApartmentUnitsCount(Integer apartmentUnitsCount) { this.apartmentUnitsCount = apartmentUnitsCount; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getProofText() { return proofText; }
    public void setProofText(String proofText) { this.proofText = proofText; }
}
