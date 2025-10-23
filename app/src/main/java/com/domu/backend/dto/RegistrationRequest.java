package com.domu.backend.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class RegistrationRequest {
    private Long unitId;
    private Long roleId;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String email;
    private String password;
}
