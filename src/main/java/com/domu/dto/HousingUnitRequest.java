package com.domu.dto;

import java.math.BigDecimal;

public class HousingUnitRequest {
    private String number;
    private String tower;
    private String floor;
    private BigDecimal aliquotPercentage;
    private BigDecimal squareMeters;

    public HousingUnitRequest() {
    }

    public HousingUnitRequest(String number, String tower, String floor, BigDecimal aliquotPercentage,
            BigDecimal squareMeters) {
        this.number = number;
        this.tower = tower;
        this.floor = floor;
        this.aliquotPercentage = aliquotPercentage;
        this.squareMeters = squareMeters;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTower() {
        return tower;
    }

    public void setTower(String tower) {
        this.tower = tower;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public BigDecimal getAliquotPercentage() {
        return aliquotPercentage;
    }

    public void setAliquotPercentage(BigDecimal aliquotPercentage) {
        this.aliquotPercentage = aliquotPercentage;
    }

    public BigDecimal getSquareMeters() {
        return squareMeters;
    }

    public void setSquareMeters(BigDecimal squareMeters) {
        this.squareMeters = squareMeters;
    }
}
