package com.domu.dto;

public record VisitorQrResponse(
    String fullName,
    String documentNumber,
    boolean exists
) {}
