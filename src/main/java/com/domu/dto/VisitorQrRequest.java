package com.domu.dto;

public class VisitorQrRequest {
    private String run;
    private String type;
    private String serial;
    private String mrz;

    public VisitorQrRequest() {
    }

    public VisitorQrRequest(String run, String type, String serial, String mrz) {
        this.run = run;
        this.type = type;
        this.serial = serial;
        this.mrz = mrz;
    }

    public String getRun() {
        return run;
    }

    public void setRun(String run) {
        this.run = run;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getMrz() {
        return mrz;
    }

    public void setMrz(String mrz) {
        this.mrz = mrz;
    }
}
