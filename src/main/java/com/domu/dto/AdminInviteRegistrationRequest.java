package com.domu.dto;

import lombok.Data;

@Data
public class AdminInviteRegistrationRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String documentNumber;
    private String password;
}
