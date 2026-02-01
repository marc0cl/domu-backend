package com.domu.dto;

public class ConfirmationRequest {
    private String token;

    public ConfirmationRequest() {}

    public ConfirmationRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
