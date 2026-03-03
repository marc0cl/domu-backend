package com.domu.dto;

import java.time.LocalDate;

public class RegistrationRequest {
    private Long unitId;       // usado en registro normal
    private Long unitNumber;   // usado en creacion admin (numero del depto)
    private Long roleId;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String email;
    private String phone;
    private String documentNumber;
    private Boolean resident;
    private String password;

    public RegistrationRequest() {}

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public Long getUnitNumber() { return unitNumber; }
    public void setUnitNumber(Long unitNumber) { this.unitNumber = unitNumber; }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public Boolean getResident() { return resident; }
    public void setResident(Boolean resident) { this.resident = resident; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
