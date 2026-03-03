package com.domu.dto;

public class AdminInviteRegistrationRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String documentNumber;
    private String password;

    public AdminInviteRegistrationRequest() {}

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
