package com.domu.email;

public interface EmailService {

    void sendHtml(String to, String subject, String htmlBody);

    void sendHtmlWithAttachment(String to, String subject, String htmlBody, String attachmentName, String attachmentContentType, byte[] attachmentContent);
}

