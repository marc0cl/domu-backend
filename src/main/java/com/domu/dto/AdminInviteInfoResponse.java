package com.domu.dto;

public record AdminInviteInfoResponse(
                String communityName,
                String adminEmail,
                String firstName,
                String lastName,
                String phone,
                String documentNumber) {
}
