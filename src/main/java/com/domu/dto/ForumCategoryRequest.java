package com.domu.dto;

import jakarta.validation.constraints.NotBlank;

public record ForumCategoryRequest(
        @NotBlank String name,
        String description
) {
}
