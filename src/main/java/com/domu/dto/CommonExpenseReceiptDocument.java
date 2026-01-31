package com.domu.dto;

public record CommonExpenseReceiptDocument(
        String fileName,
        String contentType,
        byte[] content
) {
}
