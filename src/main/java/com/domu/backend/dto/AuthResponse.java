package com.domu.backend.dto;

import com.domu.backend.dto.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
