package com.domu.dto;

import com.domu.dto.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
