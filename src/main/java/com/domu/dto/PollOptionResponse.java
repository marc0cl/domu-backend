package com.domu.dto;

public record PollOptionResponse(
        Long id,
        String label,
        Integer votes,
        Double percentage) {
}
